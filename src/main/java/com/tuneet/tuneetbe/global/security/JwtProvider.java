package com.tuneet.tuneetbe.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final String TOKEN_TYPE = "ACCESS_TOKEN";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH_TOKEN";

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    @Getter
    private final long refreshTokenExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {

        // JWT 서명에 사용할 SecretKey 생성
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        // Access Token 만료 시간
        this.accessTokenExpiration = accessTokenExpiration;

        // Refresh Token 만료 시간
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + refreshTokenExpiration);

        // Refresh Token 생성
        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // 토큰 고유값
                .subject(String.valueOf(userId)) // 토큰 주인 식별값
                .claim("type", REFRESH_TOKEN_TYPE) // 토큰 타입
                .issuedAt(now) // 발급 시간
                .expiration(expiredAt) // 만료 시간
                .signWith(secretKey) // 서명
                .compact();
    }

    public boolean validateRefreshToken(String token) {
        try {
            // 토큰 서명, 만료 시간, 타입 검증
            return REFRESH_TOKEN_TYPE.equals(extractClaims(token).get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    public String createAccessToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + accessTokenExpiration);

        // 사용자 권한 목록 추출
        List<String> roles =
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();

        // Access Token 생성
        return Jwts.builder()
                .subject(String.valueOf(userDetails.getUserId())) // 토큰 주인 식별값
                .claim("type", TOKEN_TYPE) // 토큰 타입
                .claim("email", userDetails.getUsername()) // 사용자 이메일
                .claim("roles", roles) // 사용자 권한
                .issuedAt(now) // 발급 시간
                .expiration(expiredAt) // 만료 시간
                .signWith(secretKey) // 서명
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            // 토큰 서명, 만료 시간, 타입 검증
            Claims claims = extractClaims(token);
            String type = claims.get("type", String.class);

            return TOKEN_TYPE.equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        // 토큰에서 userId 추출
        return Long.valueOf(extractClaims(token).getSubject());
    }

    private Claims extractClaims(String token) {
        // 토큰을 파싱하고 Payload 내용 추출
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
