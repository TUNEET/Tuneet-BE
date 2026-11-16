package com.tuneet.tuneetbe.domain.platform.service;

import com.tuneet.tuneetbe.domain.music.client.MusicApiClient;
import com.tuneet.tuneetbe.domain.music.dto.request.VideoMatchItem;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicSearchItemResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.VideoMatchResponse;
import com.tuneet.tuneetbe.domain.music.entity.Artist;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.entity.YoutubeVideoMatch;
import com.tuneet.tuneetbe.domain.music.repository.YoutubeVideoMatchRepository;
import com.tuneet.tuneetbe.domain.music.service.MusicCatalogService;
import com.tuneet.tuneetbe.domain.platform.client.GoogleOAuthClient;
import com.tuneet.tuneetbe.domain.platform.client.YoutubeClient;
import com.tuneet.tuneetbe.domain.platform.dto.response.PlatformSyncResponse;
import com.tuneet.tuneetbe.domain.platform.exception.PlatformErrorCode;
import com.tuneet.tuneetbe.domain.platform.util.PlatformTokenCipher;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.entity.UserPlatform;
import com.tuneet.tuneetbe.domain.user.enums.LikeSource;
import com.tuneet.tuneetbe.domain.user.enums.Platform;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserMusicLikeRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserPlatformRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 유튜브 뮤직 좋아요를 불러와 곡과 매칭하고 사용자의 좋아요로 저장한다.
 * Google·음악 API 호출은 트랜잭션 밖에서 하고, 좋아요 교체만 짧은 트랜잭션으로 묶는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeMusicSyncService {

    static final int MATCH_BATCH_SIZE = 100;
    static final int ARTIST_CANDIDATE_COUNT = 3;

    private final UserRepository userRepository;
    private final UserPlatformRepository userPlatformRepository;
    private final UserMusicLikeRepository userMusicLikeRepository;
    private final YoutubeVideoMatchRepository youtubeVideoMatchRepository;
    private final GoogleOAuthClient googleOAuthClient;
    private final YoutubeClient youtubeClient;
    private final MusicApiClient musicApiClient;
    private final MusicCatalogService musicCatalogService;
    private final PlatformTokenCipher cipher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    // 좋아요 불러오기
    public PlatformSyncResponse sync(Long userId) {
        UserPlatform platform = userPlatformRepository.findByUserIdAndPlatform(userId, Platform.YOUTUBE_MUSIC)
                .orElseThrow(() -> new CustomException(PlatformErrorCode.PLATFORM_NOT_CONNECTED));

        String accessToken = refreshAccessToken(userId, platform);
        List<VideoMatchItem> videos = youtubeClient.getLikedMusicVideos(accessToken);

        LocalDateTime now = LocalDateTime.now(clock);
        Map<String, LikedSong> songsByVideo = matchVideos(videos, now);

        // 좋아요 목록 순서(최신순)대로, 곡을 찾은 영상만
        List<LikedSong> likedSongs = videos.stream()
                .map(video -> songsByVideo.get(video.videoId()))
                .filter(Objects::nonNull)
                .toList();
        Set<Long> musicIds = new LinkedHashSet<>();
        likedSongs.forEach(song -> musicIds.add(song.musicId()));

        Boolean saved = transactionTemplate.execute(status -> replaceLikes(userId, musicIds, now));
        if (!Boolean.TRUE.equals(saved)) {
            // 불러오는 사이 연동을 해제함
            throw new CustomException(PlatformErrorCode.PLATFORM_NOT_CONNECTED);
        }

        log.info("[YoutubeMusicSyncService] 좋아요 불러오기 성공: userId = {}, videos = {}, musics = {}",
                userId, videos.size(), musicIds.size());
        return PlatformSyncResponse.builder()
                .likedMusicCount(musicIds.size())
                .skippedVideoCount(videos.size() - likedSongs.size())
                .artistCandidates(topArtists(likedSongs))
                .build();
    }

    // 사용자가 Google에서 권한을 해제했으면 연동과 불러온 좋아요를 삭제
    private String refreshAccessToken(Long userId, UserPlatform platform) {
        try {
            return googleOAuthClient.refreshAccessToken(cipher.decrypt(platform.getRefreshToken()));
        } catch (CustomException e) {
            if (e.getErrorCode() == PlatformErrorCode.PLATFORM_AUTH_EXPIRED) {
                transactionTemplate.executeWithoutResult(status -> {
                    userMusicLikeRepository.deleteAllByUserIdAndSource(userId, LikeSource.YOUTUBE_MUSIC);
                    userPlatformRepository.findByUserIdAndPlatform(userId, Platform.YOUTUBE_MUSIC)
                            .ifPresent(userPlatformRepository::delete);
                });
                log.info("[YoutubeMusicSyncService] 권한 해제로 연동 삭제: userId = {}", userId);
            }
            throw e;
        }
    }

    // 30일 안에 매칭한 영상은 저장된 결과를 쓰고, 나머지만 음악 API로 매칭
    private Map<String, LikedSong> matchVideos(List<VideoMatchItem> videos, LocalDateTime now) {
        Map<String, LikedSong> songsByVideo = new HashMap<>();
        List<String> videoIds = videos.stream().map(VideoMatchItem::videoId).distinct().toList();
        if (videoIds.isEmpty()) {
            return songsByVideo;
        }
        for (YoutubeVideoMatch match : youtubeVideoMatchRepository.findAllByVideoIdIn(videoIds)) {
            if (match.isFresh(now)) {
                songsByVideo.put(match.getVideoId(), LikedSong.from(match));
            }
        }

        Map<String, VideoMatchItem> toMatch = new LinkedHashMap<>();
        videos.stream()
                .filter(video -> !songsByVideo.containsKey(video.videoId()))
                .forEach(video -> toMatch.putIfAbsent(video.videoId(), video));
        List<VideoMatchItem> pending = List.copyOf(toMatch.values());

        for (int from = 0; from < pending.size(); from += MATCH_BATCH_SIZE) {
            List<VideoMatchItem> batch = pending.subList(from, Math.min(from + MATCH_BATCH_SIZE, pending.size()));
            List<VideoMatchResponse.Result> matched = musicApiClient.matchVideos(batch).results().stream()
                    .filter(result -> result.isMatched() && result.videoId() != null)
                    .toList();
            if (matched.isEmpty()) {
                continue;
            }
            Map<String, Music> musics = musicCatalogService.getOrCreateMusicsFrom(
                    matched.stream().map(VideoMatchResponse.Result::music).toList());
            for (VideoMatchResponse.Result result : matched) {
                Music music = musics.get(result.music().id());
                if (music == null) {
                    continue;
                }
                youtubeVideoMatchRepository.upsert(result.videoId(), music.getId(), result.isCover(), now);
                songsByVideo.put(result.videoId(), LikedSong.from(music, result));
            }
        }
        return songsByVideo;
    }

    // 이번 목록과 달라진 좋아요만 삭제·추가 (전체 삭제 후 저장하지 않음)
    private boolean replaceLikes(Long userId, Set<Long> musicIds, LocalDateTime now) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        if (user.isDeleted()) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }
        Optional<UserPlatform> platform = userPlatformRepository.findByUserIdAndPlatform(userId, Platform.YOUTUBE_MUSIC);
        if (platform.isEmpty()) {
            return false;
        }

        Set<Long> existing = new HashSet<>(
                userMusicLikeRepository.findMusicIdsByUserIdAndSource(userId, LikeSource.YOUTUBE_MUSIC));
        List<Long> removed = existing.stream().filter(id -> !musicIds.contains(id)).toList();
        if (!removed.isEmpty()) {
            userMusicLikeRepository.deleteByUserIdAndSourceAndMusicIdIn(userId, LikeSource.YOUTUBE_MUSIC, removed);
        }
        for (Long musicId : musicIds) {
            if (!existing.contains(musicId)) {
                userMusicLikeRepository.insertIfAbsent(userId, musicId, LikeSource.YOUTUBE_MUSIC.name());
            }
        }
        platform.get().markSynced(now);
        return true;
    }

    // 좋아요에서 가장 많이 나온 대표 아티스트 (커버 제외, 같은 곡은 한 번만, 동률이면 목록에서 먼저 나온 순)
    private List<PlatformSyncResponse.ArtistCandidate> topArtists(List<LikedSong> likedSongs) {
        Set<String> seenSongs = new HashSet<>();
        Map<String, PlatformSyncResponse.ArtistCandidate> tally = new LinkedHashMap<>();
        for (LikedSong song : likedSongs) {
            if (song.cover() || song.artistId() == null || !seenSongs.add(song.canonicalId())) {
                continue;
            }
            tally.merge(song.artistId(),
                    new PlatformSyncResponse.ArtistCandidate(song.artistId(), song.artistName(), 1),
                    (found, next) -> new PlatformSyncResponse.ArtistCandidate(
                            found.getArtistId(), found.getName(), found.getLikedSongs() + 1));
        }
        return tally.values().stream()
                .sorted(Comparator.comparingInt(PlatformSyncResponse.ArtistCandidate::getLikedSongs).reversed())
                .limit(ARTIST_CANDIDATE_COUNT)
                .toList();
    }

    // 좋아요 영상 하나에 매칭된 곡 (artistId는 대표 아티스트 MBID)
    record LikedSong(Long musicId, String canonicalId, String artistId, String artistName, boolean cover) {

        static LikedSong from(YoutubeVideoMatch match) {
            Music music = match.getMusic();
            Artist artist = music.getPrimaryArtist();
            return new LikedSong(music.getId(), music.getCanonicalId(),
                    artist == null ? null : artist.getExternalId(),
                    artist == null ? null : artist.getName(),
                    match.getIsCover());
        }

        static LikedSong from(Music music, VideoMatchResponse.Result result) {
            List<MusicSearchItemResponse.MusicArtist> artists = result.music().artists();
            MusicSearchItemResponse.MusicArtist first = artists == null || artists.isEmpty() ? null : artists.get(0);
            return new LikedSong(music.getId(), music.getCanonicalId(),
                    first == null ? null : first.id(),
                    first == null ? null : first.name(),
                    result.isCover());
        }
    }
}
