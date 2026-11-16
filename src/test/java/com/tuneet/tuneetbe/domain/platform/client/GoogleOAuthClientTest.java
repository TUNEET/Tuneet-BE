package com.tuneet.tuneetbe.domain.platform.client;

import com.sun.net.httpserver.HttpServer;
import com.tuneet.tuneetbe.domain.platform.exception.PlatformErrorCode;
import com.tuneet.tuneetbe.global.exception.CustomException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleOAuthClientTest {

    private HttpServer server;
    private GoogleOAuthClient client;
    private final AtomicReference<String> requestBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        client = new GoogleOAuthClient("https://accounts.google.com/o/oauth2/v2/auth", base + "/token", base + "/revoke",
                "client-id", "client-secret", "https://www.tuneet.site/oauth/youtube/callback");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void respond(String path, int status, String body) {
        server.createContext(path, exchange -> {
            requestBody.set(URLDecoder.decode(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    @Test
    @DisplayName("동의 화면 주소에 읽기 권한, 오프라인 접근, state를 담는다")
    void authorizeUrl() {
        var params = UriComponentsBuilder.fromUriString(client.authorizeUrl("abc.def")).build().getQueryParams();

        assertThat(params.getFirst("client_id")).isEqualTo("client-id");
        assertThat(params.getFirst("access_type")).isEqualTo("offline");
        assertThat(params.getFirst("prompt")).isEqualTo("consent");
        assertThat(params.getFirst("state")).isEqualTo("abc.def");
        assertThat(URLDecoder.decode(params.getFirst("scope"), StandardCharsets.UTF_8))
                .isEqualTo(GoogleOAuthClient.YOUTUBE_READONLY_SCOPE);
        assertThat(URLDecoder.decode(params.getFirst("redirect_uri"), StandardCharsets.UTF_8))
                .isEqualTo("https://www.tuneet.site/oauth/youtube/callback");
    }

    @Test
    @DisplayName("인가 코드를 보내고 토큰과 권한 범위를 받는다")
    void exchangeCode() {
        respond("/token", 200, """
                {"access_token":"ya29.a","refresh_token":"1//r","expires_in":3599,
                "scope":"openid https://www.googleapis.com/auth/youtube.readonly","token_type":"Bearer"}
                """);

        var token = client.exchangeCode("4/code");

        assertThat(requestBody.get()).contains("code=4/code").contains("grant_type=authorization_code")
                .contains("client_secret=client-secret");
        assertThat(token.accessToken()).isEqualTo("ya29.a");
        assertThat(token.refreshToken()).isEqualTo("1//r");
        assertThat(token.hasScope(GoogleOAuthClient.YOUTUBE_READONLY_SCOPE)).isTrue();
    }

    @Test
    @DisplayName("Google이 코드를 거부하면 인증 실패, refresh token을 거부하면 연동 해제로 본다")
    void rejected() {
        respond("/token", 400, "{\"error\":\"invalid_grant\"}");

        assertThatThrownBy(() -> client.exchangeCode("used-code"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlatformErrorCode.PLATFORM_AUTH_FAILED));
        assertThatThrownBy(() -> client.refreshAccessToken("revoked"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlatformErrorCode.PLATFORM_AUTH_EXPIRED));
    }

    @Test
    @DisplayName("Google 서버 오류는 통신 실패로 본다")
    void serverError() {
        respond("/token", 503, "{}");

        assertThatThrownBy(() -> client.refreshAccessToken("token"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlatformErrorCode.PLATFORM_API_UNAVAILABLE));
    }

    @Test
    @DisplayName("토큰 폐기는 실패해도 예외를 던지지 않는다")
    void revoke() {
        respond("/revoke", 200, "{}");
        assertThat(client.revoke("1//r")).isTrue();
        assertThat(requestBody.get()).isEqualTo("token=1//r");

        server.removeContext("/revoke");
        respond("/revoke", 400, "{\"error\":\"invalid_token\"}");
        assertThat(client.revoke("1//r")).isFalse();
    }
}
