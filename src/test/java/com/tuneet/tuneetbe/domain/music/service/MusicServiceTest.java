package com.tuneet.tuneetbe.domain.music.service;

import com.tuneet.tuneetbe.domain.music.client.MusicApiClient;
import com.tuneet.tuneetbe.domain.music.dto.response.GenreResponse;
import com.tuneet.tuneetbe.domain.music.entity.Genre;
import com.tuneet.tuneetbe.domain.music.enums.GenreType;
import com.tuneet.tuneetbe.domain.music.repository.GenreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MusicServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private MusicApiClient musicApiClient;

    @InjectMocks
    private MusicService musicService;

    private Genre genre(long id, GenreType type) throws Exception {
        Constructor<Genre> constructor = Genre.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Genre genre = constructor.newInstance();
        ReflectionTestUtils.setField(genre, "id", id);
        ReflectionTestUtils.setField(genre, "genreName", type);
        return genre;
    }

    @Test
    @DisplayName("장르를 화면 순서로 조회하고 표시 이름을 포함한다")
    void getGenres() throws Exception {
        given(genreRepository.findAll(Sort.by("displayOrder")))
                .willReturn(List.of(genre(4L, GenreType.R_AND_B_SOUL)));

        List<GenreResponse> responses = musicService.getGenres();

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.getGenreId()).isEqualTo(4L);
            assertThat(response.getGenreName()).isEqualTo(GenreType.R_AND_B_SOUL);
            assertThat(response.getDisplayName()).isEqualTo("R&B / SOUL");
        });
    }
}
