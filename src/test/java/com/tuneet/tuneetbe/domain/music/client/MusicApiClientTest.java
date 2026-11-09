package com.tuneet.tuneetbe.domain.music.client;

import com.sun.net.httpserver.HttpServer;
import com.tuneet.tuneetbe.domain.music.exception.MusicErrorCode;
import com.tuneet.tuneetbe.global.exception.CustomException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MusicApiClientTest {

    private static final String MUSIC_BODY = """
            {"items":[{"id":"r1","canonicalId":"c1","title":"봄날","artist":"BTS",
            "artists":[{"id":"a1","name":"BTS"}],"album":null,"albumType":null,"durationMs":null,
            "releaseYear":2017,"coverUrl":null,"isVariant":false}],"page":2,"size":5,"hasNext":true,"totalPages":3}
            """;

    private HttpServer server;
    private MusicApiClient client;
    private final AtomicReference<String> apiKey = new AtomicReference<>();
    private final AtomicReference<String> rawQuery = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        client = new MusicApiClient("http://127.0.0.1:" + server.getAddress().getPort(), "test-key");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void respond(String path, int status, String body) {
        server.createContext(path, exchange -> {
            apiKey.set(exchange.getRequestHeaders().getFirst("X-Api-Key"));
            rawQuery.set(exchange.getRequestURI().getRawQuery());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    @Test
    @DisplayName("노래 검색은 API 키와 인코딩된 검색어를 보내고 응답을 변환한다")
    void searchMusics() {
        respond("/v1/musics", 200, MUSIC_BODY);

        var response = client.searchMusics("봄날 & BTS", 2, 5);

        assertThat(apiKey.get()).isEqualTo("test-key");
        assertThat(rawQuery.get()).contains("q=%EB%B4%84%EB%82%A0%20%26%20BTS").contains("page=2").contains("size=5");
        assertThat(response.hasNext()).isTrue();
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.canonicalId()).isEqualTo("c1");
            assertThat(item.artists().get(0).name()).isEqualTo("BTS");
            assertThat(item.coverUrl()).isNull();
        });
    }

    @Test
    @DisplayName("아티스트 검색 응답을 변환한다")
    void searchArtists() {
        respond("/v1/artists", 200, """
                {"items":[{"id":"a1","name":"IU","disambiguation":null,"songCount":320}],
                "page":1,"size":5,"hasNext":false,"totalPages":1}
                """);

        var response = client.searchArtists("아이유", 1, 5);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.name()).isEqualTo("IU");
            assertThat(item.songCount()).isEqualTo(320);
        });
    }

    @Test
    @DisplayName("음악 API가 오류를 응답하면 MUSIC_API_UNAVAILABLE 예외가 발생한다")
    void errorResponse() {
        respond("/v1/musics", 401, "{\"status\":401,\"message\":\"unauthorized\"}");

        assertThatThrownBy(() -> client.searchMusics("봄날", 1, 5))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(MusicErrorCode.MUSIC_API_UNAVAILABLE);
    }

    @Test
    @DisplayName("음악 API에 연결할 수 없으면 MUSIC_API_UNAVAILABLE 예외가 발생한다")
    void connectionFailure() {
        server.stop(0);

        assertThatThrownBy(() -> client.searchArtists("아이유", 1, 5))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(MusicErrorCode.MUSIC_API_UNAVAILABLE);
    }
}
