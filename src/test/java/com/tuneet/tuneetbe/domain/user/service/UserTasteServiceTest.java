package com.tuneet.tuneetbe.domain.user.service;

import com.tuneet.tuneetbe.domain.music.entity.Artist;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.exception.MusicErrorCode;
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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserTasteServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMusicRepository userMusicRepository;

    @Mock
    private UserArtistRepository userArtistRepository;

    @Mock
    private MusicCatalogService musicCatalogService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private UserTasteService service;

    @BeforeEach
    void setUp() {
        service = new UserTasteService(userRepository, userMusicRepository, userArtistRepository,
                musicCatalogService, new TransactionTemplate(transactionManager));
    }

    private LinkedHashSet<String> ids(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    private User user() {
        return BeanUtils.instantiateClass(User.class);
    }

    private Music music(long id) {
        Music music = BeanUtils.instantiateClass(Music.class);
        ReflectionTestUtils.setField(music, "id", id);
        ReflectionTestUtils.setField(music, "title", "제목" + id);
        return music;
    }

    private Artist artist(long id) {
        Artist artist = BeanUtils.instantiateClass(Artist.class);
        ReflectionTestUtils.setField(artist, "id", id);
        ReflectionTestUtils.setField(artist, "name", "가수" + id);
        return artist;
    }

    @Test
    @DisplayName("좋아하는 노래는 달라진 항목만 삭제하고 저장하며 요청 순서대로 돌려준다")
    void replaceMusics() {
        LinkedHashSet<String> ids = ids("r2", "r1");
        User user = user();
        UserMusic kept = UserMusic.builder().user(user).music(music(1)).build();
        UserMusic removed = UserMusic.builder().user(user).music(music(3)).build();
        given(musicCatalogService.getOrCreateMusics(ids)).willReturn(List.of(music(2), music(1)));
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(userMusicRepository.findAllByUserId(USER_ID)).willReturn(List.of(kept, removed));

        MusicReplaceResponse response = service.replaceMusics(USER_ID, new MusicReplaceRequest(ids));

        verify(userMusicRepository).deleteAllInBatch(List.of(removed));
        ArgumentCaptor<List<UserMusic>> saved = ArgumentCaptor.captor();
        verify(userMusicRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(um -> um.getMusic().getId()).containsExactly(2L);
        assertThat(response.getMusics()).extracting("musicId").containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("선호 아티스트는 달라진 항목만 삭제하고 저장하며 요청 순서대로 돌려준다")
    void replaceArtists() {
        LinkedHashSet<String> ids = ids("a3", "a1", "a2");
        User user = user();
        UserArtist kept = UserArtist.builder().user(user).artist(artist(1)).build();
        UserArtist removed = UserArtist.builder().user(user).artist(artist(9)).build();
        given(musicCatalogService.getOrCreateArtists(ids)).willReturn(List.of(artist(3), artist(1), artist(2)));
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(userArtistRepository.findAllByUserId(USER_ID)).willReturn(List.of(kept, removed));

        ArtistReplaceResponse response = service.replaceArtists(USER_ID, new ArtistReplaceRequest(ids));

        verify(userArtistRepository).deleteAllInBatch(List.of(removed));
        ArgumentCaptor<List<UserArtist>> saved = ArgumentCaptor.captor();
        verify(userArtistRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(ua -> ua.getArtist().getId()).containsExactly(3L, 2L);
        assertThat(response.getArtists()).extracting("artistId").containsExactly(3L, 1L, 2L);
    }

    @Test
    @DisplayName("곡 저장에 실패하면 기존 좋아하는 노래를 지우지 않는다")
    void keepsMusicsWhenCatalogFails() {
        LinkedHashSet<String> ids = ids("r404");
        given(musicCatalogService.getOrCreateMusics(ids)).willThrow(new CustomException(MusicErrorCode.MUSIC_NOT_FOUND));

        assertThatThrownBy(() -> service.replaceMusics(USER_ID, new MusicReplaceRequest(ids)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(MusicErrorCode.MUSIC_NOT_FOUND);
        verify(userMusicRepository, never()).deleteAllInBatch(any());
    }

    @Test
    @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
    void userNotFound() {
        LinkedHashSet<String> ids = ids("a1", "a2", "a3");
        given(musicCatalogService.getOrCreateArtists(ids)).willReturn(List.of(artist(1), artist(2), artist(3)));
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.replaceArtists(USER_ID, new ArtistReplaceRequest(ids)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        verify(userArtistRepository, never()).deleteAllInBatch(any());
    }
}
