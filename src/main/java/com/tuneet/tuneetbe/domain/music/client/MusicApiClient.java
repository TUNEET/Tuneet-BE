package com.tuneet.tuneetbe.domain.music.client;

import com.tuneet.tuneetbe.domain.music.dto.response.ArtistSearchItemResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicSearchItemResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.SearchPageResponse;
import com.tuneet.tuneetbe.domain.music.exception.MusicErrorCode;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Component
public class MusicApiClient {

    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient client;

    public MusicApiClient(
            @Value("${music-api.base-url}") String baseUrl,
            @Value("${music-api.api-key}") String apiKey) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(API_KEY_HEADER, apiKey)
                .requestFactory(requestFactory)
                .build();
    }

    public SearchPageResponse<MusicSearchItemResponse> searchMusics(String query, int page, int size) {
        return search("/v1/musics", query, page, size, new ParameterizedTypeReference<>() {
        });
    }

    public SearchPageResponse<ArtistSearchItemResponse> searchArtists(String query, int page, int size) {
        return search("/v1/artists", query, page, size, new ParameterizedTypeReference<>() {
        });
    }

    private <T> SearchPageResponse<T> search(
            String path, String query, int page, int size,
            ParameterizedTypeReference<SearchPageResponse<T>> responseType) {
        try {
            return client.get()
                    .uri(uri -> uri.path(path)
                            .queryParam("q", "{query}")
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .build(query))
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException e) {
            log.error("음악 API 호출 실패: {} - {}", path, e.getMessage());
            throw new CustomException(MusicErrorCode.MUSIC_API_UNAVAILABLE);
        }
    }
}
