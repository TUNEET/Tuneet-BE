package com.tuneet.tuneetbe.domain.user.service;

import com.tuneet.tuneetbe.domain.music.dto.response.ArtistResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.music.entity.Artist;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.service.MusicCatalogService;
import com.tuneet.tuneetbe.domain.user.dto.request.ArtistReplaceRequest;
import com.tuneet.tuneetbe.domain.user.dto.request.MusicReplaceRequest;
import com.tuneet.tuneetbe.domain.user.dto.response.ArtistReplaceResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.MusicReplaceResponse;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.entity.UserArtist;
import com.tuneet.tuneetbe.domain.user.entity.UserMusic;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserArtistRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserMusicRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 곡·아티스트 저장은 음악 API를 호출하므로 트랜잭션 밖에서 끝내고,
 * 사용자 취향 교체만 하나의 트랜잭션으로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserTasteService {

    private final UserRepository userRepository;
    private final UserMusicRepository userMusicRepository;
    private final UserArtistRepository userArtistRepository;
    private final MusicCatalogService musicCatalogService;
    private final TransactionTemplate transactionTemplate;

    // 좋아하는 노래 교체
    public MusicReplaceResponse replaceMusics(Long userId, MusicReplaceRequest request) {
        List<Music> musics = musicCatalogService.getOrCreateMusics(request.getMusicIds());

        Set<Long> selectedIds = musics.stream().map(Music::getId).collect(Collectors.toSet());
        transactionTemplate.executeWithoutResult(status -> {
            User user = findUserForUpdate(userId);
            List<UserMusic> current = userMusicRepository.findAllByUserId(userId);
            Set<Long> currentIds = current.stream().map(um -> um.getMusic().getId()).collect(Collectors.toSet());

            // 달라진 행만 변경 (빈 범위를 지우면 gap lock으로 신규 사용자끼리 교착 상태가 생긴다)
            userMusicRepository.deleteAllInBatch(current.stream()
                    .filter(um -> !selectedIds.contains(um.getMusic().getId()))
                    .toList());
            userMusicRepository.saveAll(musics.stream()
                    .filter(music -> !currentIds.contains(music.getId()))
                    .map(music -> UserMusic.builder().user(user).music(music).build())
                    .toList());
        });

        log.info("[UserTasteService] 좋아하는 노래 교체 성공: userId = {}", userId);
        return MusicReplaceResponse.builder()
                .musics(musics.stream().map(this::toMusicResponse).toList())
                .build();
    }

    // 선호 아티스트 교체
    public ArtistReplaceResponse replaceArtists(Long userId, ArtistReplaceRequest request) {
        List<Artist> artists = musicCatalogService.getOrCreateArtists(request.getArtistIds());

        Set<Long> selectedIds = artists.stream().map(Artist::getId).collect(Collectors.toSet());
        transactionTemplate.executeWithoutResult(status -> {
            User user = findUserForUpdate(userId);
            List<UserArtist> current = userArtistRepository.findAllByUserId(userId);
            Set<Long> currentIds = current.stream().map(ua -> ua.getArtist().getId()).collect(Collectors.toSet());

            userArtistRepository.deleteAllInBatch(current.stream()
                    .filter(ua -> !selectedIds.contains(ua.getArtist().getId()))
                    .toList());
            userArtistRepository.saveAll(artists.stream()
                    .filter(artist -> !currentIds.contains(artist.getId()))
                    .map(artist -> UserArtist.builder().user(user).artist(artist).build())
                    .toList());
        });

        log.info("[UserTasteService] 선호 아티스트 교체 성공: userId = {}", userId);
        return ArtistReplaceResponse.builder()
                .artists(artists.stream().map(this::toArtistResponse).toList())
                .build();
    }

    // 같은 사용자의 중복 요청이 순서대로 처리되도록 사용자 행을 잠그고 조회
    private User findUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private MusicResponse toMusicResponse(Music music) {
        return MusicResponse.builder()
                .musicId(music.getExternalId())
                .title(music.getTitle())
                .artist(music.getArtist())
                .musicPicture(music.getMusicPicture())
                .build();
    }

    private ArtistResponse toArtistResponse(Artist artist) {
        return ArtistResponse.builder()
                .artistId(artist.getExternalId())
                .name(artist.getName())
                .artistImage(artist.getArtistImage())
                .build();
    }
}
