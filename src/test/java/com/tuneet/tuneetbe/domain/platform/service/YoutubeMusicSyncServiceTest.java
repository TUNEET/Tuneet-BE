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
import com.tuneet.tuneetbe.domain.user.repository.UserMusicLikeRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserPlatformRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class YoutubeMusicSyncServiceTest {

    private static final Long USER_ID = 1L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-16T00:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPlatformRepository userPlatformRepository;

    @Mock
    private UserMusicLikeRepository userMusicLikeRepository;

    @Mock
    private YoutubeVideoMatchRepository youtubeVideoMatchRepository;

    @Mock
    private GoogleOAuthClient googleOAuthClient;

    @Mock
    private YoutubeClient youtubeClient;

    @Mock
    private MusicApiClient musicApiClient;

    @Mock
    private MusicCatalogService musicCatalogService;

    @Mock
    private PlatformTokenCipher cipher;

    @Mock
    private PlatformTransactionManager transactionManager;

    private YoutubeMusicSyncService service;
    private UserPlatform platform;

    @BeforeEach
    void setUp() {
        service = new YoutubeMusicSyncService(userRepository, userPlatformRepository, userMusicLikeRepository,
                youtubeVideoMatchRepository, googleOAuthClient, youtubeClient, musicApiClient, musicCatalogService,
                cipher, new TransactionTemplate(transactionManager), CLOCK);
        platform = UserPlatform.builder().platform(Platform.YOUTUBE_MUSIC).refreshToken("encrypted").build();
    }

    private void connected() {
        given(userPlatformRepository.findByUserIdAndPlatform(USER_ID, Platform.YOUTUBE_MUSIC)).willReturn(Optional.of(platform));
        given(cipher.decrypt("encrypted")).willReturn("refresh");
        given(googleOAuthClient.refreshAccessToken("refresh")).willReturn("access");
    }

    private Artist artist(String mbid, String name) {
        Artist artist = BeanUtils.instantiateClass(Artist.class);
        ReflectionTestUtils.setField(artist, "externalId", mbid);
        ReflectionTestUtils.setField(artist, "name", name);
        return artist;
    }

    private Music music(long id, String canonicalId, Artist artist) {
        Music music = BeanUtils.instantiateClass(Music.class);
        ReflectionTestUtils.setField(music, "id", id);
        ReflectionTestUtils.setField(music, "externalId", "r" + id);
        ReflectionTestUtils.setField(music, "canonicalId", canonicalId);
        ReflectionTestUtils.setField(music, "primaryArtist", artist);
        return music;
    }

    private MusicSearchItemResponse item(String id, String canonicalId, String artistId, String artistName) {
        return new MusicSearchItemResponse(id, canonicalId, "제목", artistName,
                List.of(new MusicSearchItemResponse.MusicArtist(artistId, artistName)), null, null, null, null);
    }

    @Test
    @DisplayName("저장된 매칭은 다시 매칭하지 않고, 달라진 좋아요만 삭제·추가하며 아티스트 후보를 센다")
    void sync() {
        connected();
        Artist bts = artist("a-bts", "BTS");
        Music cached = music(10, "c10", bts);
        given(youtubeClient.getLikedMusicVideos("access")).willReturn(List.of(
                new VideoMatchItem("v1", "Dynamite", "BTS - Topic"),
                new VideoMatchItem("v2", "Butter", "BTS - Topic"),
                new VideoMatchItem("v3", "Hype Boy", "NewJeans - Topic"),
                new VideoMatchItem("v4", "모음", "어떤 채널")));
        given(youtubeVideoMatchRepository.findAllByVideoIdIn(List.of("v1", "v2", "v3", "v4"))).willReturn(List.of(
                YoutubeVideoMatch.builder().videoId("v1").music(cached).isCover(false).matchedAt(NOW.minusDays(3)).build(),
                // 30일이 지난 매칭은 다시 매칭
                YoutubeVideoMatch.builder().videoId("v2").music(music(11, "c11", bts)).isCover(false)
                        .matchedAt(NOW.minusDays(31)).build()));
        MusicSearchItemResponse butter = item("r11", "c11", "a-bts", "BTS");
        MusicSearchItemResponse hypeBoy = item("r12", "c12", "a-nj", "NewJeans");
        given(musicApiClient.matchVideos(List.of(
                new VideoMatchItem("v2", "Butter", "BTS - Topic"),
                new VideoMatchItem("v3", "Hype Boy", "NewJeans - Topic"),
                new VideoMatchItem("v4", "모음", "어떤 채널"))))
                .willReturn(new VideoMatchResponse(List.of(
                        new VideoMatchResponse.Result("v2", "matched", false, null, butter),
                        new VideoMatchResponse.Result("v3", "matched", false, null, hypeBoy),
                        new VideoMatchResponse.Result("v4", "unmatched", false, "compilation", null))));
        given(musicCatalogService.getOrCreateMusicsFrom(List.of(butter, hypeBoy)))
                .willReturn(Map.of("r11", music(11, "c11", null), "r12", music(12, "c12", null)));
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(BeanUtils.instantiateClass(User.class)));
        // 기존 좋아요: 10은 유지, 99는 유튜브에서 취소됨
        given(userMusicLikeRepository.findMusicIdsByUserIdAndSource(USER_ID, LikeSource.YOUTUBE_MUSIC))
                .willReturn(List.of(10L, 99L));

        PlatformSyncResponse response = service.sync(USER_ID);

        verify(youtubeVideoMatchRepository).upsert("v2", 11L, false, NOW);
        verify(youtubeVideoMatchRepository).upsert("v3", 12L, false, NOW);
        verify(userMusicLikeRepository).deleteByUserIdAndSourceAndMusicIdIn(USER_ID, LikeSource.YOUTUBE_MUSIC, List.of(99L));
        verify(userMusicLikeRepository).insertIfAbsent(USER_ID, 11L, "YOUTUBE_MUSIC");
        verify(userMusicLikeRepository).insertIfAbsent(USER_ID, 12L, "YOUTUBE_MUSIC");
        verify(userMusicLikeRepository, never()).insertIfAbsent(USER_ID, 10L, "YOUTUBE_MUSIC");
        assertThat(platform.getLastSyncedAt()).isEqualTo(NOW);

        assertThat(response.getLikedMusicCount()).isEqualTo(3);
        assertThat(response.getSkippedVideoCount()).isEqualTo(1);
        assertThat(response.getArtistCandidates()).extracting("artistId", "likedSongs")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("a-bts", 2), org.assertj.core.groups.Tuple.tuple("a-nj", 1));
    }

    @Test
    @DisplayName("아티스트 후보는 커버와 같은 곡의 다른 음원을 세지 않는다")
    void artistCandidatesSkipCoversAndDuplicates() {
        connected();
        Artist iu = artist("a-iu", "IU");
        Artist bts = artist("a-bts", "BTS");
        given(youtubeClient.getLikedMusicVideos("access")).willReturn(List.of(
                new VideoMatchItem("v1", "Spring Day", "BTS - Topic"),
                new VideoMatchItem("v2", "Spring Day (Live)", "BTS - Topic"),
                new VideoMatchItem("v3", "밤편지 cover", "누군가"),
                new VideoMatchItem("v4", "밤편지 cover 2", "다른 사람"),
                new VideoMatchItem("v5", "Blueming", "IU - Topic")));
        given(youtubeVideoMatchRepository.findAllByVideoIdIn(any())).willReturn(List.of(
                YoutubeVideoMatch.builder().videoId("v1").music(music(1, "spring", bts)).isCover(false).matchedAt(NOW).build(),
                YoutubeVideoMatch.builder().videoId("v2").music(music(2, "spring", bts)).isCover(false).matchedAt(NOW).build(),
                YoutubeVideoMatch.builder().videoId("v3").music(music(3, "letter", iu)).isCover(true).matchedAt(NOW).build(),
                YoutubeVideoMatch.builder().videoId("v4").music(music(3, "letter", iu)).isCover(true).matchedAt(NOW).build(),
                YoutubeVideoMatch.builder().videoId("v5").music(music(5, "blueming", iu)).isCover(false).matchedAt(NOW).build()));
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(BeanUtils.instantiateClass(User.class)));

        PlatformSyncResponse response = service.sync(USER_ID);

        verify(musicApiClient, never()).matchVideos(any());
        // 같은 곡을 커버 두 개로 좋아요해도 좋아요 곡은 한 번
        assertThat(response.getLikedMusicCount()).isEqualTo(4);
        assertThat(response.getArtistCandidates()).extracting("artistId", "likedSongs")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("a-bts", 1), org.assertj.core.groups.Tuple.tuple("a-iu", 1));
    }

    @Test
    @DisplayName("Google에서 권한을 해제했으면 연동과 불러온 좋아요를 삭제한다")
    void authExpired() {
        given(userPlatformRepository.findByUserIdAndPlatform(USER_ID, Platform.YOUTUBE_MUSIC)).willReturn(Optional.of(platform));
        given(cipher.decrypt("encrypted")).willReturn("refresh");
        given(googleOAuthClient.refreshAccessToken("refresh"))
                .willThrow(new CustomException(PlatformErrorCode.PLATFORM_AUTH_EXPIRED));

        assertThatThrownBy(() -> service.sync(USER_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlatformErrorCode.PLATFORM_AUTH_EXPIRED));
        verify(userMusicLikeRepository).deleteAllByUserIdAndSource(USER_ID, LikeSource.YOUTUBE_MUSIC);
        verify(userPlatformRepository).delete(platform);
        verify(youtubeClient, never()).getLikedMusicVideos(anyString());
    }

    @Test
    @DisplayName("Google 통신 오류면 연동을 유지한다")
    void googleUnavailable() {
        given(userPlatformRepository.findByUserIdAndPlatform(USER_ID, Platform.YOUTUBE_MUSIC)).willReturn(Optional.of(platform));
        given(cipher.decrypt("encrypted")).willReturn("refresh");
        given(googleOAuthClient.refreshAccessToken("refresh"))
                .willThrow(new CustomException(PlatformErrorCode.PLATFORM_API_UNAVAILABLE));

        assertThatThrownBy(() -> service.sync(USER_ID)).isInstanceOf(CustomException.class);
        verify(userPlatformRepository, never()).delete(any());
    }

    @Test
    @DisplayName("불러오는 사이 연동을 해제했으면 좋아요를 저장하지 않는다")
    void unlinkedDuringSync() {
        given(userPlatformRepository.findByUserIdAndPlatform(USER_ID, Platform.YOUTUBE_MUSIC))
                .willReturn(Optional.of(platform), Optional.empty());
        given(cipher.decrypt("encrypted")).willReturn("refresh");
        given(googleOAuthClient.refreshAccessToken("refresh")).willReturn("access");
        given(youtubeClient.getLikedMusicVideos("access")).willReturn(List.of());
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(BeanUtils.instantiateClass(User.class)));

        assertThatThrownBy(() -> service.sync(USER_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlatformErrorCode.PLATFORM_NOT_CONNECTED));
        verify(userMusicLikeRepository, never()).insertIfAbsent(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("연동하지 않았으면 불러올 수 없다")
    void notConnected() {
        given(userPlatformRepository.findByUserIdAndPlatform(USER_ID, Platform.YOUTUBE_MUSIC)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.sync(USER_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlatformErrorCode.PLATFORM_NOT_CONNECTED));
    }

    @Test
    @DisplayName("음악 API에는 100개씩 나눠 보낸다")
    void matchInBatches() {
        connected();
        List<VideoMatchItem> videos = java.util.stream.IntStream.range(0, 150)
                .mapToObj(i -> new VideoMatchItem("v" + i, "t" + i, "c")).toList();
        given(youtubeClient.getLikedMusicVideos("access")).willReturn(videos);
        given(youtubeVideoMatchRepository.findAllByVideoIdIn(any())).willReturn(List.of());
        given(musicApiClient.matchVideos(any())).willReturn(new VideoMatchResponse(List.of()));
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(BeanUtils.instantiateClass(User.class)));

        service.sync(USER_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VideoMatchItem>> batches = ArgumentCaptor.forClass(List.class);
        verify(musicApiClient, org.mockito.Mockito.times(2)).matchVideos(batches.capture());
        assertThat(batches.getAllValues()).extracting(List::size).containsExactly(100, 50);
    }
}
