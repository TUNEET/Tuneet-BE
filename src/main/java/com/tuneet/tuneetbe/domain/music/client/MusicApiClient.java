package com.tuneet.tuneetbe.domain.music.client;

import com.tuneet.tuneetbe.domain.music.dto.response.ArtistSearchItemResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicBatchResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicSearchItemResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.SearchPageResponse;
import com.tuneet.tuneetbe.domain.music.exception.MusicErrorCode;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    /** At most 100 recording MBIDs. Unknown IDs are returned in missingIds. */
    public MusicBatchResponse getMusics(Collection<String> ids) {
        String path = "/v1/musics/batch";
        try {
            return requireBody(path, client.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new MusicBatchRequest(List.copyOf(ids)))
                    .retrieve()
                    .body(MusicBatchResponse.class));
        } catch (RestClientException e) {
            throw unavailable(path, e);
        }
    }

    public Optional<ArtistSearchItemResponse> getArtist(String id) {
        String path = "/v1/artists/{id}";
        try {
            return Optional.of(requireBody(path, client.get()
                    .uri(path, id)
                    .retrieve()
                    .body(ArtistSearchItemResponse.class)));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw unavailable(path, e);
        }
    }

    private <T> SearchPageResponse<T> search(
            String path, String query, int page, int size,
            ParameterizedTypeReference<SearchPageResponse<T>> responseType) {
        try {
            return requireBody(path, client.get()
                    .uri(uri -> uri.path(path)
                            .queryParam("q", "{query}")
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .build(query))
                    .retrieve()
                    .body(responseType));
        } catch (RestClientException e) {
            throw unavailable(path, e);
        }
    }

    private <T> T requireBody(String path, T body) {
        if (body == null) {
            log.error("음악 API 응답 본문 없음: {}", path);
            throw new CustomException(MusicErrorCode.MUSIC_API_UNAVAILABLE);
        }
        return body;
    }

    private CustomException unavailable(String path, RestClientException e) {
        log.error("음악 API 호출 실패: {} - {}", path, e.getMessage());
        return new CustomException(MusicErrorCode.MUSIC_API_UNAVAILABLE);
    }

    private record MusicBatchRequest(List<String> ids) {
    }
}
