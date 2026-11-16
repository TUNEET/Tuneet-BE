package com.tuneet.tuneetbe.domain.platform.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tuneet.tuneetbe.domain.platform.exception.PlatformErrorCode;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Component
public class GoogleOAuthClient {

    public static final String YOUTUBE_READONLY_SCOPE = "https://www.googleapis.com/auth/youtube.readonly";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient client;
    private final String authUri;
    private final String tokenUri;
    private final String revokeUri;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GoogleOAuthClient(
            @Value("${google.oauth.auth-uri:https://accounts.google.com/o/oauth2/v2/auth}") String authUri,
            @Value("${google.oauth.token-uri:https://oauth2.googleapis.com/token}") String tokenUri,
            @Value("${google.oauth.revoke-uri:https://oauth2.googleapis.com/revoke}") String revokeUri,
            @Value("${google.oauth.client-id:}") String clientId,
            @Value("${google.oauth.client-secret:}") String clientSecret,
            @Value("${google.oauth.redirect-uri:}") String redirectUri) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.client = RestClient.builder().requestFactory(requestFactory).build();
        this.authUri = authUri;
        this.tokenUri = tokenUri;
        this.revokeUri = revokeUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    // 동의 화면 주소 (prompt=consent: 다시 연동해도 refresh token을 받기 위해)
    public String authorizeUrl(String state) {
        return UriComponentsBuilder.fromUriString(authUri)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", YOUTUBE_READONLY_SCOPE)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("include_granted_scopes", "true")
                .queryParam("state", state)
                .encode()
                .toUriString();
    }

    // 인가 코드를 토큰으로 교환
    public TokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");
        return requestToken(form, PlatformErrorCode.PLATFORM_AUTH_FAILED);
    }

    // refresh token으로 access token 발급 (권한이 해제됐으면 PLATFORM_AUTH_EXPIRED)
    public String refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("refresh_token", refreshToken);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "refresh_token");
        return requestToken(form, PlatformErrorCode.PLATFORM_AUTH_EXPIRED).accessToken();
    }

    // 토큰 폐기 (실패해도 연동 해제는 진행하므로 결과만 반환)
    public boolean revoke(String token) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);
        try {
            client.post()
                    .uri(revokeUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.warn("Google 토큰 폐기 실패: {}", e.getMessage());
            return false;
        }
    }

    private TokenResponse requestToken(MultiValueMap<String, String> form, PlatformErrorCode rejected) {
        try {
            TokenResponse response = client.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw new CustomException(PlatformErrorCode.PLATFORM_API_UNAVAILABLE);
            }
            return response;
        } catch (HttpClientErrorException e) {
            // invalid_grant: 코드가 만료·재사용됐거나 사용자가 권한을 해제함
            log.warn("Google 토큰 요청 거부: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (rejected == PlatformErrorCode.PLATFORM_AUTH_EXPIRED && !isInvalidGrant(e)) {
                // invalid_client 등 서버 설정 오류로 연동을 지우지 않음
                throw new CustomException(PlatformErrorCode.PLATFORM_API_UNAVAILABLE);
            }
            throw new CustomException(rejected);
        } catch (RestClientException e) {
            log.error("Google 토큰 요청 실패: {}", e.getMessage());
            throw new CustomException(PlatformErrorCode.PLATFORM_API_UNAVAILABLE);
        }
    }

    private boolean isInvalidGrant(HttpClientErrorException e) {
        try {
            TokenError error = e.getResponseBodyAs(TokenError.class);
            return error != null && "invalid_grant".equals(error.error());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    record TokenError(String error) {
    }

    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") Long expiresIn,
            String scope) {

        public boolean hasScope(String required) {
            return scope != null && java.util.Arrays.asList(scope.split(" ")).contains(required);
        }
    }
}
