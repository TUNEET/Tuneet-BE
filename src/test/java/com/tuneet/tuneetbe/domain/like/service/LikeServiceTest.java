package com.tuneet.tuneetbe.domain.like.service;

import com.tuneet.tuneetbe.domain.like.dto.response.LikedArtistListResponse;
import com.tuneet.tuneetbe.domain.like.dto.response.LikedMusicListResponse;
import com.tuneet.tuneetbe.domain.like.entity.UserArtistLike;
import com.tuneet.tuneetbe.domain.like.repository.UserArtistLikeRepository;
import com.tuneet.tuneetbe.domain.music.entity.Artist;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.exception.MusicErrorCode;
import com.tuneet.tuneetbe.domain.music.service.MusicCatalogService;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.entity.UserMusicLike;
import com.tuneet.tuneetbe.domain.user.enums.LikeSource;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserMusicLikeRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
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
class LikeServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMusicLikeRepository userMusicLikeRepository;

    @Mock
    private UserArtistLikeRepository userArtistLikeRepository;

    @Mock
    private MusicCatalogService musicCatalogService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private LikeService service;

    @BeforeEach
    void setUp() {
        service = new LikeService(userRepository, userMusicLikeRepository, userArtistLikeRepository,
                musicCatalogService, new TransactionTemplate(transactionManager));
    }

    private User user() {
        User user = User.builder().email("a@email.com").password("pw")
                .termsAgreedAt(LocalDateTime.now()).privacyAgreedAt(LocalDateTime.now()).build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private Music music(Long id, String externalId) {
        Music music = BeanUtils.instantiateClass(Music.class);
        ReflectionTestUtils.setField(music, "id", id);
        ReflectionTestUtils.setField(music, "externalId", externalId);
        ReflectionTestUtils.setField(music, "title", "Hype Boy");
        return music;
    }

    private Artist artist(Long id, String externalId) {
        Artist artist = BeanUtils.instantiateClass(Artist.class);
        ReflectionTestUtils.setField(artist, "id", id);
        ReflectionTestUtils.setField(artist, "externalId", externalId);
        ReflectionTestUtils.setField(artist, "name", "NewJeans");
        return artist;
    }

    @Test
    @DisplayName("곡 좋아요는 곡을 가져와 앱 좋아요로 저장한다 (이미 있으면 그대로)")
    void likeMusic() {
        given(musicCatalogService.getOrCreateMusics(List.of("rec-a"))).willReturn(List.of(music(10L, "rec-a")));
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user()));

        service.likeMusic(USER_ID, "rec-a");

        verify(userMusicLikeRepository).insertIfAbsent(USER_ID, 10L, "IN_APP");
    }

    @Test
    @DisplayName("없는 곡이면 저장하지 않는다")
    void likeUnknownMusic() {
        given(musicCatalogService.getOrCreateMusics(List.of("nope"))).willThrow(new CustomException(MusicErrorCode.MUSIC_NOT_FOUND));

        assertThatThrownBy(() -> service.likeMusic(USER_ID, "nope")).isInstanceOf(CustomException.class);
        verify(userMusicLikeRepository, never()).insertIfAbsent(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("음악 API를 기다리는 사이 탈퇴했으면 좋아요를 저장하지 않는다")
    void likeAfterWithdrawal() {
        User deleted = user();
        deleted.deleteUser(LocalDateTime.now());
        given(musicCatalogService.getOrCreateMusics(List.of("rec-a"))).willReturn(List.of(music(10L, "rec-a")));
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(deleted));

        assertThatThrownBy(() -> service.likeMusic(USER_ID, "rec-a"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND));
        verify(userMusicLikeRepository, never()).insertIfAbsent(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("곡 좋아요 취소는 같은 곡의 좋아요를 출처 상관없이 지우고, 없으면 아무것도 하지 않는다")
    void unlikeMusic() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user()));
        given(userMusicLikeRepository.findIdsByUserIdAndSameSongAs(USER_ID, "rec-a")).willReturn(List.of(3L, 4L));

        service.unlikeMusic(USER_ID, "rec-a");
        service.unlikeMusic(USER_ID, "never-liked");

        verify(userMusicLikeRepository).deleteAllByIdInBatch(List.of(3L, 4L));
        verify(userMusicLikeRepository, never()).deleteAllByIdInBatch(List.of());
    }

    @Test
    @DisplayName("아티스트 좋아요와 취소")
    void artistLike() {
        given(musicCatalogService.getOrCreateArtists(List.of("art-1"))).willReturn(List.of(artist(20L, "art-1")));
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user()));
        given(userArtistLikeRepository.findIdsByUserIdAndArtistExternalId(USER_ID, "art-1")).willReturn(List.of(5L));

        service.likeArtist(USER_ID, "art-1");
        service.unlikeArtist(USER_ID, "art-1");

        verify(userArtistLikeRepository).insertIfAbsent(USER_ID, 20L);
        verify(userArtistLikeRepository).deleteAllByIdInBatch(List.of(5L));
    }

    @Test
    @DisplayName("좋아요한 곡 목록은 출처와 함께 liked=true로 반환한다")
    void likedMusics() {
        UserMusicLike like = BeanUtils.instantiateClass(UserMusicLike.class);
        ReflectionTestUtils.setField(like, "id", 42L);
        ReflectionTestUtils.setField(like, "music", music(10L, "rec-a"));
        ReflectionTestUtils.setField(like, "source", LikeSource.YOUTUBE_MUSIC);
        given(userMusicLikeRepository.findLatestPerSong(USER_ID, 50L, PageRequest.of(0, 20)))
                .willReturn(new SliceImpl<>(List.of(like), PageRequest.of(0, 20), true));

        LikedMusicListResponse response = service.getLikedMusics(USER_ID, 50L, 20);

        assertThat(response.getHasNext()).isTrue();
        assertThat(response.getMusics()).singleElement().satisfies(item -> {
            assertThat(item.getLikeId()).isEqualTo(42L);
            assertThat(item.getSource()).isEqualTo(LikeSource.YOUTUBE_MUSIC);
            assertThat(item.getMusic().getMusicId()).isEqualTo("rec-a");
            assertThat(item.getMusic().getLiked()).isTrue();
        });
    }

    @Test
    @DisplayName("좋아요한 아티스트 목록은 cursor가 없으면 처음부터, 있으면 그 이전부터 조회한다")
    void likedArtists() {
        UserArtistLike like = BeanUtils.instantiateClass(UserArtistLike.class);
        ReflectionTestUtils.setField(like, "id", 7L);
        ReflectionTestUtils.setField(like, "artist", artist(20L, "art-1"));
        given(userArtistLikeRepository.findByUserIdOrderByIdDesc(USER_ID, PageRequest.of(0, 20)))
                .willReturn(new SliceImpl<>(List.of(like), PageRequest.of(0, 20), false));
        given(userArtistLikeRepository.findByUserIdAndIdLessThanOrderByIdDesc(USER_ID, 7L, PageRequest.of(0, 20)))
                .willReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 20), false));

        LikedArtistListResponse first = service.getLikedArtists(USER_ID, null, 20);
        service.getLikedArtists(USER_ID, 7L, 20);

        assertThat(first.getArtists()).singleElement().satisfies(item -> {
            assertThat(item.getLikeId()).isEqualTo(7L);
            assertThat(item.getArtist().getArtistId()).isEqualTo("art-1");
        });
    }
}
