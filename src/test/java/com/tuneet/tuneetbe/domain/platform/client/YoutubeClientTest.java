package com.tuneet.tuneetbe.domain.platform.client;

import com.sun.net.httpserver.HttpServer;
import com.tuneet.tuneetbe.domain.music.dto.request.VideoMatchItem;
import com.tuneet.tuneetbe.domain.platform.exception.PlatformErrorCode;
import com.tuneet.tuneetbe.global.exception.CustomException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YoutubeClientTest {

    private HttpServer server;
    private YoutubeClient client;
    private final List<String> queries = new ArrayList<>();
    private final List<String> authorizations = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        client = new YoutubeClient("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void respond(int status, String... pages) {
        server.createContext("/playlistItems", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            queries.add(query);
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            String body = query.contains("pageToken=") ? pages[1] : pages[0];
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    @Test
    @DisplayName("LM 재생목록을 다음 페이지까지 읽고 삭제·비공개 영상은 건너뛴다")
    void likedMusicVideos() {
        respond(200, """
                {"nextPageToken":"P2","items":[
                {"snippet":{"title":"Dynamite","videoOwnerChannelTitle":"BTS - Topic","resourceId":{"videoId":"v1"}}},
                {"snippet":{"title":"Deleted video","resourceId":{"videoId":"v2"}}}]}
                """, """
                {"items":[{"snippet":{"title":"Hype Boy","videoOwnerChannelTitle":"NewJeans - Topic","resourceId":{"videoId":"v3"}}}]}
                """);

        List<VideoMatchItem> videos = client.getLikedMusicVideos("ya29.token");

        assertThat(videos).containsExactly(
                new VideoMatchItem("v1", "Dynamite", "BTS - Topic"),
                new VideoMatchItem("v3", "Hype Boy", "NewJeans - Topic"));
        assertThat(queries).hasSize(2);
        assertThat(queries.get(0)).contains("playlistId=LM").contains("maxResults=50").doesNotContain("pageToken");
        assertThat(queries.get(1)).contains("pageToken=P2");
        assertThat(authorizations).containsOnly("Bearer ya29.token");
    }

    @Test
    @DisplayName("다음 페이지 응답이 비면 일부 목록을 돌려주지 않는다")
    void emptyNextPage() {
        respond(200, """
                {"nextPageToken":"P2","items":[
                {"snippet":{"title":"Dynamite","videoOwnerChannelTitle":"BTS - Topic","resourceId":{"videoId":"v1"}}}]}
                """, "");

        assertThatThrownBy(() -> client.getLikedMusicVideos("token"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlatformErrorCode.PLATFORM_API_UNAVAILABLE));
    }

    @Test
    @DisplayName("좋아요 재생목록이 없으면 빈 목록이다")
    void noLikedPlaylist() {
        respond(404, "{\"error\":{\"code\":404}}");

        assertThat(client.getLikedMusicVideos("token")).isEmpty();
    }

    @Test
    @DisplayName("인증이 거부되면 인증 실패, 할당량 초과 등은 통신 실패로 본다")
    void errors() {
        respond(401, "{}");
        assertThatThrownBy(() -> client.getLikedMusicVideos("token"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlatformErrorCode.PLATFORM_AUTH_FAILED));

        server.removeContext("/playlistItems");
        respond(403, "{\"error\":{\"errors\":[{\"reason\":\"quotaExceeded\"}]}}");
        assertThatThrownBy(() -> client.getLikedMusicVideos("token"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlatformErrorCode.PLATFORM_API_UNAVAILABLE));
    }

    private void respondVideos(int status, String body) {
        server.createContext("/videos", exchange -> {
            queries.add(exchange.getRequestURI().getRawQuery());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    @Test
    @DisplayName("영상 길이와 설명의 발매 연도를 읽는다")
    void videoDetails() {
        respondVideos(200, """
                {"items":[
                {"id":"v1","contentDetails":{"duration":"PT4M30S"},
                 "snippet":{"description":"Provided to YouTube by Universal Music Group\\n\\nHeroine · back number\\n\\nReleased on: 2015-12-09"}},
                {"id":"v2","contentDetails":{"duration":"PT3M5S"},"snippet":{"description":"설명 없음"}},
                {"id":"v3","contentDetails":{"duration":"P0D"},"snippet":{}}]}
                """);

        var details = client.getVideoDetails("ya29.token", List.of("v1", "v2", "v3"));

        assertThat(details).containsOnlyKeys("v1", "v2");
        assertThat(details.get("v1")).isEqualTo(new YoutubeClient.VideoDetails(270, 2015));
        assertThat(details.get("v2")).isEqualTo(new YoutubeClient.VideoDetails(185, null));
        assertThat(queries.get(0)).contains("part=contentDetails,snippet").contains("id=v1%2Cv2%2Cv3").doesNotContain("maxResults");
    }

    @Test
    @DisplayName("영상 정보 조회가 실패하면 빈 결과를 돌려준다")
    void videoDetailsFailure() {
        respondVideos(403, "{\"error\":{\"code\":403}}");

        assertThat(client.getVideoDetails("ya29.token", List.of("v1"))).isEmpty();
    }
}
