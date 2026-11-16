package com.tuneet.tuneetbe.domain.matching.service;

import com.tuneet.tuneetbe.domain.like.entity.UserArtistLike;
import com.tuneet.tuneetbe.domain.like.repository.UserArtistLikeRepository;
import com.tuneet.tuneetbe.domain.music.entity.Artist;
import com.tuneet.tuneetbe.domain.music.entity.Genre;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.entity.UserArtist;
import com.tuneet.tuneetbe.domain.user.entity.UserGenre;
import com.tuneet.tuneetbe.domain.user.entity.UserMusic;
import com.tuneet.tuneetbe.domain.user.entity.UserMusicLike;
import com.tuneet.tuneetbe.domain.user.enums.DistanceRange;
import com.tuneet.tuneetbe.domain.user.entity.UserBlock;
import com.tuneet.tuneetbe.domain.user.enums.BlockType;
import com.tuneet.tuneetbe.domain.user.repository.UserArtistRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserBlockRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserGenreRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserMusicLikeRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserMusicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TasteMatchServiceTest {

    private static final double LAT = 37.5665;
    private static final double LON = 126.9780;

    @Mock
    private UserMusicRepository userMusicRepository;

    @Mock
    private UserMusicLikeRepository userMusicLikeRepository;

    @Mock
    private UserArtistRepository userArtistRepository;

    @Mock
    private UserGenreRepository userGenreRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserArtistLikeRepository userArtistLikeRepository;

    private TasteMatchService service;

    @BeforeEach
    void setUp() {
        service = new TasteMatchService(userMusicRepository, userMusicLikeRepository, userArtistRepository, userGenreRepository,
                userBlockRepository, userArtistLikeRepository);
    }

    // 위치 동의, 위치 노출을 켜고 위치를 저장한 사용자
    private User user(Long id) {
        User user = User.builder().email(id + "@email.com").password("pw")
                .termsAgreedAt(LocalDateTime.now()).privacyAgreedAt(LocalDateTime.now())
                .locationAgreedAt(LocalDateTime.now()).build();
        ReflectionTestUtils.setField(user, "id", id);
        user.updateLocation(LAT, LON, LocalDateTime.now());
        return user;
    }

    private Music music(String externalId, String canonicalId) {
        Music music = BeanUtils.instantiateClass(Music.class);
        ReflectionTestUtils.setField(music, "externalId", externalId);
        ReflectionTestUtils.setField(music, "canonicalId", canonicalId);
        return music;
    }

    private UserMusicLike like(User user, Music music) {
        UserMusicLike like = BeanUtils.instantiateClass(UserMusicLike.class);
        ReflectionTestUtils.setField(like, "user", user);
        ReflectionTestUtils.setField(like, "music", music);
        return like;
    }

    private UserArtist userArtist(User user, Long artistId) {
        Artist artist = BeanUtils.instantiateClass(Artist.class);
        ReflectionTestUtils.setField(artist, "id", artistId);
        return UserArtist.builder().user(user).artist(artist).build();
    }

    private UserGenre userGenre(User user, Long genreId) {
        Genre genre = BeanUtils.instantiateClass(Genre.class);
        ReflectionTestUtils.setField(genre, "id", genreId);
        ReflectionTestUtils.setField(genre, "displayOrder", genreId.intValue());
        return UserGenre.builder().user(user).genre(genre).build();
    }

    @Test
    @DisplayName("곡 1, 아티스트 1, 장르 3/3이 겹치면 60%이고 같은 곡의 다른 음원은 한 번만 센다")
    void matchPercent() {
        User me = user(1L);
        User other = user(2L);
        given(userMusicRepository.findCanonicalIdsByUserId(1L)).willReturn(List.of("song-1"));
        given(userMusicRepository.findAllByUserIdInAndMusicCanonicalIdInOrderByIdAsc(any(), any()))
                .willReturn(List.of(UserMusic.builder().user(other).music(music("recording-a", "song-1")).build()));
        given(userMusicLikeRepository.findAllByUserIdInAndMusicCanonicalIdInOrderByIdAsc(any(), any()))
                .willReturn(List.of(like(other, music("recording-b", "song-1"))));
        given(userArtistRepository.findArtistIdsByUserId(1L)).willReturn(List.of(7L));
        given(userArtistRepository.findAllByUserIdInAndArtistIdInOrderByIdAsc(any(), any()))
                .willReturn(List.of(userArtist(other, 7L)));
        given(userGenreRepository.findAllByUserIdIn(anyCollection())).willReturn(List.of(
                userGenre(me, 1L), userGenre(me, 2L), userGenre(me, 3L),
                userGenre(other, 1L), userGenre(other, 2L), userGenre(other, 3L)));

        TasteMatch match = service.match(me, List.of(other)).get(2L);

        assertThat(match.matchPercent()).isEqualTo(60);
        assertThat(match.commonMusics()).extracting(Music::getExternalId).containsExactly("recording-a");
        assertThat(match.commonArtists()).hasSize(1);
        assertThat(match.commonGenres()).hasSize(3);
        assertThat(match.distanceRange()).isEqualTo(DistanceRange.WITHIN_1KM);
    }

    @Test
    @DisplayName("선호 아티스트와 좋아요한 아티스트를 합쳐 비교하고 같은 아티스트는 한 번만 센다")
    void artistLikes() {
        User me = user(1L);
        User other = user(2L);
        given(userArtistRepository.findArtistIdsByUserId(1L)).willReturn(List.of(7L));
        given(userArtistLikeRepository.findArtistIdsByUserId(1L)).willReturn(List.of(8L));
        given(userArtistRepository.findAllByUserIdInAndArtistIdInOrderByIdAsc(any(), any()))
                .willReturn(List.of(userArtist(other, 8L)));
        UserArtistLike otherLike = BeanUtils.instantiateClass(UserArtistLike.class);
        ReflectionTestUtils.setField(otherLike, "user", other);
        ReflectionTestUtils.setField(otherLike, "artist", userArtist(other, 8L).getArtist());
        UserArtistLike otherLike7 = BeanUtils.instantiateClass(UserArtistLike.class);
        ReflectionTestUtils.setField(otherLike7, "user", other);
        ReflectionTestUtils.setField(otherLike7, "artist", userArtist(other, 7L).getArtist());
        given(userArtistLikeRepository.findAllByUserIdInAndArtistIdInOrderByIdAsc(any(), any()))
                .willReturn(List.of(otherLike, otherLike7));
        given(userArtistLikeRepository.findUserIdsHavingArtistLike(any())).willReturn(List.of(2L));

        TasteMatch match = service.match(me, List.of(other)).get(2L);

        // 아티스트 8(선호·좋아요 중복), 7 → 2명, 0.4 * 0.75 = 30%
        assertThat(match.commonArtists()).extracting(Artist::getId).containsExactly(8L, 7L);
        assertThat(match.matchPercent()).isEqualTo(30);
    }

    @Test
    @DisplayName("곡, 아티스트는 개수가 늘수록 1에 가까워지고 장르는 겹치는 비율로 계산한다")
    void formula() {
        assertThat(TasteMatchService.matchPercent(0, 0, 0, 0)).isZero();
        // 0.4 * 0.75 + 0.4 * 0.875 + 0.2 * 1/4 = 0.70
        assertThat(TasteMatchService.matchPercent(2, 3, 1, 4)).isEqualTo(70);
        assertThat(TasteMatchService.matchPercent(30, 30, 2, 2)).isEqualTo(100);
    }

    @Test
    @DisplayName("상대에게 취향 데이터가 있지만 겹치는 게 없으면 0%이다")
    void noOverlap() {
        User me = user(1L);
        User other = user(2L);
        given(userGenreRepository.findAllByUserIdIn(anyCollection()))
                .willReturn(List.of(userGenre(me, 1L), userGenre(other, 5L)));

        assertThat(service.match(me, List.of(other)).get(2L).matchPercent()).isZero();
    }

    @Test
    @DisplayName("상대의 취향 데이터가 하나도 없으면 매칭률은 null이다")
    void otherWithoutTaste() {
        User me = user(1L);
        User other = user(2L);
        given(userGenreRepository.findAllByUserIdIn(anyCollection())).willReturn(List.of(userGenre(me, 1L)));

        assertThat(service.match(me, List.of(other)).get(2L).matchPercent()).isNull();
    }

    @Test
    @DisplayName("내가 위치 노출을 껐으면 매칭률과 거리 모두 null이다")
    void myLocationHidden() {
        User me = user(1L);
        me.updateSettings(false, null);
        User other = user(2L);
        given(userGenreRepository.findAllByUserIdIn(anyCollection()))
                .willReturn(List.of(userGenre(me, 1L), userGenre(other, 1L)));

        TasteMatch match = service.match(me, List.of(other)).get(2L);

        assertThat(match.matchPercent()).isNull();
        assertThat(match.distanceRange()).isNull();
        assertThat(match.commonGenres()).hasSize(1);
        verify(userArtistRepository, never()).findUserIdsHavingArtist(any());
    }

    @Test
    @DisplayName("상대가 위치 노출을 껐거나 위치가 없으면 거리만 null이다")
    void otherLocationHidden() {
        User me = user(1L);
        User hidden = user(2L);
        hidden.updateSettings(false, null);
        User noLocation = user(3L);
        noLocation.updateLocation(null, null, null);
        User near = user(4L);
        near.updateLocation(LAT + 0.02, LON, LocalDateTime.now());
        given(userGenreRepository.findAllByUserIdIn(anyCollection())).willReturn(List.of(
                userGenre(me, 1L), userGenre(hidden, 1L), userGenre(noLocation, 1L), userGenre(near, 1L)));

        Map<Long, TasteMatch> matches = service.match(me, List.of(hidden, noLocation, near));

        assertThat(matches.get(2L).distanceRange()).isNull();
        assertThat(matches.get(2L).matchPercent()).isEqualTo(20);
        assertThat(matches.get(3L).distanceRange()).isNull();
        assertThat(matches.get(4L).distanceRange()).isEqualTo(DistanceRange.WITHIN_3KM);
    }

    @Test
    @DisplayName("상대가 나를 차단했거나 내가 상대를 차단했으면 거리는 null이다")
    void blocked() {
        User me = user(1L);
        User blockedMe = user(2L);
        User blockedByMe = user(3L);
        User other = user(4L);
        given(userBlockRepository.findAllByBlockerIdOrBlockedId(1L, 1L)).willReturn(List.of(
                UserBlock.builder().blocker(blockedMe).blocked(me).type(BlockType.BLOCK).build(),
                UserBlock.builder().blocker(me).blocked(blockedByMe).type(BlockType.BLOCK).build()));

        Map<Long, TasteMatch> matches = service.match(me, List.of(blockedMe, blockedByMe, other));

        assertThat(matches.get(2L).distanceRange()).isNull();
        assertThat(matches.get(3L).distanceRange()).isNull();
        assertThat(matches.get(4L).distanceRange()).isEqualTo(DistanceRange.WITHIN_1KM);
    }

    @Test
    @DisplayName("본인은 결과에 넣지 않는다")
    void excludesMe() {
        User me = user(1L);

        assertThat(service.match(me, List.of(me))).isEmpty();
        verify(userGenreRepository, never()).findAllByUserIdIn(any());
    }
}
