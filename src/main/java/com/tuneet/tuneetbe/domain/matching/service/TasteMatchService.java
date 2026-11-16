package com.tuneet.tuneetbe.domain.matching.service;

import com.tuneet.tuneetbe.domain.matching.util.DistanceCalculator;
import com.tuneet.tuneetbe.domain.music.entity.Artist;
import com.tuneet.tuneetbe.domain.music.entity.Genre;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.entity.UserGenre;
import com.tuneet.tuneetbe.domain.user.enums.DistanceRange;
import com.tuneet.tuneetbe.domain.user.repository.UserArtistRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserGenreRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserMusicLikeRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserMusicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 취향 매칭률과 거리 구간을 여러 사용자에 대해 한 번에 계산한다.
 * 사용자 수와 상관없이 쿼리 수가 일정하다. (프로필, 방 멤버 목록, 추천에서 같이 사용)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TasteMatchService {

    static final double MUSIC_WEIGHT = 0.4;
    static final double ARTIST_WEIGHT = 0.4;
    static final double GENRE_WEIGHT = 0.2;

    private final UserMusicRepository userMusicRepository;
    private final UserMusicLikeRepository userMusicLikeRepository;
    private final UserArtistRepository userArtistRepository;
    private final UserGenreRepository userGenreRepository;

    public Map<Long, TasteMatch> match(User me, Collection<User> targets) {
        Map<Long, TasteMatch> result = new HashMap<>();
        List<User> others = targets.stream().filter(target -> !target.getId().equals(me.getId())).toList();
        if (others.isEmpty()) {
            return result;
        }
        List<Long> otherIds = others.stream().map(User::getId).toList();

        // 노래: 좋아하는 노래 + 좋아요한 노래 (같은 곡의 다른 음원도 같은 곡)
        Set<String> myCanonicalIds = new HashSet<>(userMusicRepository.findCanonicalIdsByUserId(me.getId()));
        myCanonicalIds.addAll(userMusicLikeRepository.findCanonicalIdsByUserId(me.getId()));
        Map<Long, Map<String, Music>> commonMusics = new HashMap<>();
        if (!myCanonicalIds.isEmpty()) {
            userMusicRepository.findAllByUserIdInAndMusicCanonicalIdInOrderByIdAsc(otherIds, myCanonicalIds)
                    .forEach(um -> addMusic(commonMusics, um.getUser().getId(), um.getMusic()));
            userMusicLikeRepository.findAllByUserIdInAndMusicCanonicalIdInOrderByIdAsc(otherIds, myCanonicalIds)
                    .forEach(like -> addMusic(commonMusics, like.getUser().getId(), like.getMusic()));
        }

        // 아티스트
        List<Long> myArtistIds = userArtistRepository.findArtistIdsByUserId(me.getId());
        Map<Long, List<Artist>> commonArtists = new HashMap<>();
        if (!myArtistIds.isEmpty()) {
            userArtistRepository.findAllByUserIdInAndArtistIdInOrderByIdAsc(otherIds, myArtistIds)
                    .forEach(ua -> commonArtists.computeIfAbsent(ua.getUser().getId(), id -> new ArrayList<>()).add(ua.getArtist()));
        }

        // 장르 (표시 순서대로)
        List<Long> genreUserIds = new ArrayList<>(otherIds);
        genreUserIds.add(me.getId());
        Map<Long, List<Genre>> genres = new HashMap<>();
        userGenreRepository.findAllByUserIdIn(genreUserIds).stream()
                .sorted(Comparator.comparing((UserGenre ug) -> ug.getGenre().getDisplayOrder(),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(ug -> genres.computeIfAbsent(ug.getUser().getId(), id -> new ArrayList<>()).add(ug.getGenre()));
        List<Genre> myGenres = genres.getOrDefault(me.getId(), List.of());
        Set<Long> myGenreIds = new HashSet<>();
        myGenres.forEach(genre -> myGenreIds.add(genre.getId()));

        // 매칭률은 내가 매칭을 쓸 수 있고 양쪽 모두 취향 데이터가 있을 때만
        boolean iHaveTaste = !myCanonicalIds.isEmpty() || !myArtistIds.isEmpty() || !myGenres.isEmpty();
        boolean showPercent = me.canUseMatching() && iHaveTaste;
        Set<Long> othersWithTaste = showPercent ? findUsersWithTaste(otherIds, genres) : Set.of();

        for (User other : others) {
            Long id = other.getId();
            List<Music> musics = List.copyOf(commonMusics.getOrDefault(id, Map.of()).values());
            List<Artist> artists = commonArtists.getOrDefault(id, List.of());
            List<Genre> otherGenres = genres.getOrDefault(id, List.of());
            List<Genre> sharedGenres = otherGenres.stream().filter(genre -> myGenreIds.contains(genre.getId())).toList();
            int genreUnion = myGenres.size() + otherGenres.size() - sharedGenres.size();

            Integer percent = othersWithTaste.contains(id)
                    ? matchPercent(musics.size(), artists.size(), sharedGenres.size(), genreUnion)
                    : null;
            result.put(id, new TasteMatch(percent, distanceRange(me, other), musics, artists, sharedGenres));
        }
        return result;
    }

    // 곡, 아티스트는 개수가 늘수록 1에 가까워지게 (직접 고른 몇 곡과 불러온 수백 곡을 비교해도 값이 너무 작아지지 않도록)
    // 장르는 겹치는 비율
    static int matchPercent(int commonMusics, int commonArtists, int commonGenres, int genreUnion) {
        double genre = genreUnion == 0 ? 0 : (double) commonGenres / genreUnion;
        double score = MUSIC_WEIGHT * saturate(commonMusics) + ARTIST_WEIGHT * saturate(commonArtists) + GENRE_WEIGHT * genre;
        return (int) Math.round(score * 100);
    }

    // 1개 0.5, 2개 0.75, 3개 0.875
    private static double saturate(int count) {
        return 1 - Math.pow(0.5, count);
    }

    private static DistanceRange distanceRange(User me, User other) {
        if (!me.hasVisibleLocation() || !other.hasVisibleLocation()) {
            return null;
        }
        return DistanceCalculator.range(me.getLatitude(), me.getLongitude(), other.getLatitude(), other.getLongitude());
    }

    private static void addMusic(Map<Long, Map<String, Music>> commonMusics, Long userId, Music music) {
        commonMusics.computeIfAbsent(userId, id -> new LinkedHashMap<>()).putIfAbsent(music.getCanonicalId(), music);
    }

    // 장르, 아티스트, 노래, 좋아요 중 하나라도 있는 사용자
    private Set<Long> findUsersWithTaste(List<Long> userIds, Map<Long, List<Genre>> genres) {
        Set<Long> withTaste = new HashSet<>(genres.keySet());
        withTaste.addAll(userArtistRepository.findUserIdsHavingArtist(userIds));
        withTaste.addAll(userMusicRepository.findUserIdsHavingMusic(userIds));
        withTaste.addAll(userMusicLikeRepository.findUserIdsHavingLike(userIds));
        return withTaste;
    }
}
