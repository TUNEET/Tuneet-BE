package com.tuneet.tuneetbe.domain.music.service;

import com.tuneet.tuneetbe.domain.music.client.MusicApiClient;
import com.tuneet.tuneetbe.domain.music.dto.response.ArtistSearchItemResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.GenreResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicSearchItemResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.SearchPageResponse;
import com.tuneet.tuneetbe.domain.music.entity.Genre;
import com.tuneet.tuneetbe.domain.music.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MusicService {

    private final GenreRepository genreRepository;
    private final MusicApiClient musicApiClient;

    // 장르 조회
    @Transactional(readOnly = true)
    public List<GenreResponse> getGenres() {
        return genreRepository.findAll(Sort.by("displayOrder")).stream()
                .map(this::toGenreResponse)
                .toList();
    }

    // 노래 검색
    public SearchPageResponse<MusicSearchItemResponse> searchMusics(String query, int page, int size) {
        return musicApiClient.searchMusics(query, page, size);
    }

    // 아티스트 검색
    public SearchPageResponse<ArtistSearchItemResponse> searchArtists(String query, int page, int size) {
        return musicApiClient.searchArtists(query, page, size);
    }

    // Genre 엔티티를 장르 응답 dto로 변환
    private GenreResponse toGenreResponse(Genre genre) {
        return GenreResponse.builder()
                .genreId(genre.getId())
                .genreName(genre.getGenreName())
                .displayName(genre.getGenreName().getDisplayName())
                .build();
    }
}
