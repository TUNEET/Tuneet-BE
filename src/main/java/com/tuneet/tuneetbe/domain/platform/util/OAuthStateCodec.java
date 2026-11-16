package com.tuneet.tuneetbe.domain.platform.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

// 연동을 시작한 사용자와 만료 시각을 서명해 state로 사용 (서버에 저장하지 않음)
@Component
@RequiredArgsConstructor
public class OAuthStateCodec {

    static final Duration TTL = Duration.ofMinutes(10);

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final PlatformTokenCipher cipher;
    private final Clock clock;

    public String create(Long userId) {
        long expiresAt = clock.instant().plus(TTL).getEpochSecond();
        String nonce = HexFormat.of().formatHex(cipher.randomBytes(16));
        byte[] payload = (userId + ":" + expiresAt + ":" + nonce).getBytes(StandardCharsets.UTF_8);
        return ENCODER.encodeToString(payload) + "." + ENCODER.encodeToString(cipher.sign(payload));
    }

    // 서명이 맞고, 만료 전이고, 로그인한 사용자가 연동을 시작한 사용자일 때만 true
    public boolean isValid(String state, Long userId) {
        try {
            String[] parts = state.split("\\.");
            if (parts.length != 2) {
                return false;
            }
            byte[] payload = DECODER.decode(parts[0]);
            if (!cipher.verify(payload, DECODER.decode(parts[1]))) {
                return false;
            }
            String[] fields = new String(payload, StandardCharsets.UTF_8).split(":");
            return fields.length == 3
                    && Long.parseLong(fields[0]) == userId
                    && Long.parseLong(fields[1]) > clock.instant().getEpochSecond();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
