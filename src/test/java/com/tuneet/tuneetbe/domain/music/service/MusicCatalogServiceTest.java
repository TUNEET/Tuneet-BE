package com.tuneet.tuneetbe.domain.music.service;

import com.tuneet.tuneetbe.domain.music.client.MusicApiClient;
import com.tuneet.tuneetbe.domain.music.dto.response.ArtistSearchItemResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicBatchResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicSearchItemResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicSearchItemResponse.MusicArtist;
import com.tuneet.tuneetbe.domain.music.entity.Artist;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.exception.MusicErrorCode;
import com.tuneet.tuneetbe.domain.music.repository.ArtistRepository;
import com.tuneet.tuneetbe.domain.music.repository.MusicRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MusicCatalogServiceTest {

    @Mock
    private MusicApiClient musicApiClient;

    @Mock
    private MusicRepository musicRepository;

    @Mock
    private ArtistRepository artistRepository;

    @InjectMocks
    private MusicCatalogService service;

    private Music music(long id, String externalId) {
        Music music = BeanUtils.instantiateClass(Music.class);
        ReflectionTestUtils.setField(music, "id", id);
        ReflectionTestUtils.setField(music, "externalId", externalId);
        return music;
    }

    private Artist artist(long id, String externalId) {
        Artist artist = BeanUtils.instantiateClass(Artist.class);
        ReflectionTestUtils.setField(artist, "id", id);
        ReflectionTestUtils.setField(artist, "externalId", externalId);
        return artist;
    }

    private MusicSearchItemResponse item(String id, MusicArtist... artists) {
        return new MusicSearchItemResponse(id, "c-" + id, "제목", "표기", List.of(artists), null, null, null, "cover");
    }

    @Test
    @DisplayName("이미 저장된 곡은 음악 API를 호출하지 않고 요청 순서대로 돌려준다")
    void existingMusics() {
        List<String> ids = List.of("r2", "r1");
        given(musicRepository.findAllByExternalIdIn(ids)).willReturn(List.of(music(1, "r1"), music(2, "r2")));

        List<Music> result = service.getOrCreateMusics(ids);

        assertThat(result).extracting(Music::getExternalId).containsExactly("r2", "r1");
        verify(musicApiClient, never()).getMusics(any());
    }

    @Test
    @DisplayName("없는 곡만 음악 API에서 가져와 대표 아티스트와 함께 저장한다")
    void createsMissingMusic() {
        List<String> ids = List.of("r1", "r2");
        MusicArtist first = new MusicArtist("a1", "IU");
        given(musicRepository.findAllByExternalIdIn(ids))
                .willReturn(List.of(music(1, "r1")))
                .willReturn(List.of(music(1, "r1"), music(2, "r2")));
        given(musicApiClient.getMusics(List.of("r2")))
                .willReturn(new MusicBatchResponse(List.of(item("r2", first, new MusicArtist("a2", "다른 가수"))), List.of()));
        given(artistRepository.findAllByExternalIdIn(any())).willReturn(List.of(artist(7, "a1")));

        List<Music> result = service.getOrCreateMusics(ids);

        assertThat(result).extracting(Music::getExternalId).containsExactly("r1", "r2");
        verify(artistRepository).insertIfAbsent("IU", "a1");
        verify(artistRepository, never()).insertIfAbsent("다른 가수", "a2");
        verify(musicRepository).insertIfAbsent("제목", "표기", "cover", "r2", "c-r2", 7L);
    }

    @Test
    @DisplayName("아티스트 연결이 없는 곡은 대표 아티스트 없이 저장한다")
    void createsMusicWithoutArtist() {
        List<String> ids = List.of("r1");
        given(musicRepository.findAllByExternalIdIn(ids))
                .willReturn(List.of())
                .willReturn(List.of(music(1, "r1")));
        given(musicApiClient.getMusics(ids)).willReturn(new MusicBatchResponse(List.of(item("r1")), List.of()));

        service.getOrCreateMusics(ids);

        verify(musicRepository).insertIfAbsent("제목", "표기", "cover", "r1", "c-r1", null);
        verify(artistRepository, never()).insertIfAbsent(anyString(), anyString());
    }

    @Test
    @DisplayName("음악 API에 없는 곡이면 저장하지 않고 MUSIC_NOT_FOUND 예외가 발생한다")
    void unknownMusic() {
        List<String> ids = List.of("r1", "r404");
        given(musicRepository.findAllByExternalIdIn(ids)).willReturn(List.of());
        given(musicApiClient.getMusics(ids)).willReturn(new MusicBatchResponse(List.of(item("r1")), List.of("r404")));

        assertThatThrownBy(() -> service.getOrCreateMusics(ids))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(MusicErrorCode.MUSIC_NOT_FOUND);
        verify(musicRepository, never()).insertIfAbsent(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("없는 아티스트만 음악 API에서 가져와 저장하고 요청 순서대로 돌려준다")
    void createsMissingArtist() {
        List<String> ids = List.of("a2", "a1");
        given(artistRepository.findAllByExternalIdIn(ids))
                .willReturn(List.of(artist(1, "a1")))
                .willReturn(List.of(artist(1, "a1"), artist(2, "a2")));
        given(musicApiClient.getArtist("a2"))
                .willReturn(Optional.of(new ArtistSearchItemResponse("a2", "RESCENE", null, 3)));

        List<Artist> result = service.getOrCreateArtists(ids);

        assertThat(result).extracting(Artist::getExternalId).containsExactly("a2", "a1");
        verify(artistRepository).insertIfAbsent("RESCENE", "a2");
        verify(musicApiClient, never()).getArtist("a1");
    }

    @Test
    @DisplayName("음악 API에 없는 아티스트면 ARTIST_NOT_FOUND 예외가 발생한다")
    void unknownArtist() {
        List<String> ids = List.of("a404");
        given(artistRepository.findAllByExternalIdIn(ids)).willReturn(List.of());
        given(musicApiClient.getArtist("a404")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrCreateArtists(ids))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(MusicErrorCode.ARTIST_NOT_FOUND);
    }
}
