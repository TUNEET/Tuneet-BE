package com.tuneet.tuneetbe.domain.platform.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthStateCodecTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final Instant NOW = Instant.parse("2026-11-16T00:00:00Z");

    private final PlatformTokenCipher cipher = new PlatformTokenCipher(KEY);

    private OAuthStateCodec codecAt(Instant instant) {
        return new OAuthStateCodec(cipher, Clock.fixed(instant, ZoneId.of("Asia/Seoul")));
    }

    @Test
    @DisplayName("refresh token은 암호화 후 복호화하면 원래 값이고, 같은 값도 매번 다르게 암호화된다")
    void encryptAndDecrypt() {
        String first = cipher.encrypt("1//refresh-token");
        String second = cipher.encrypt("1//refresh-token");

        assertThat(first).isNotEqualTo(second).doesNotContain("refresh");
        assertThat(cipher.decrypt(first)).isEqualTo("1//refresh-token");
    }

    @Test
    @DisplayName("변조된 암호문은 복호화하지 않는다")
    void rejectTamperedCipherText() {
        byte[] data = Base64.getDecoder().decode(cipher.encrypt("token"));
        data[data.length - 1] ^= 1;

        assertThatThrownBy(() -> cipher.decrypt(Base64.getEncoder().encodeToString(data)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("키가 없으면 서버는 뜨지만 암호화할 때 실패한다")
    void missingKey() {
        PlatformTokenCipher withoutKey = new PlatformTokenCipher("");

        assertThatThrownBy(() -> withoutKey.encrypt("token")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("state는 발급받은 사용자가 만료 전에 쓸 때만 유효하다")
    void validState() {
        String state = codecAt(NOW).create(1L);

        assertThat(codecAt(NOW.plusSeconds(599)).isValid(state, 1L)).isTrue();
        assertThat(codecAt(NOW).isValid(state, 2L)).isFalse();
        assertThat(codecAt(NOW.plus(OAuthStateCodec.TTL)).isValid(state, 1L)).isFalse();
    }

    @Test
    @DisplayName("서명이 맞지 않거나 형식이 틀린 state는 거부한다")
    void rejectTamperedState() {
        OAuthStateCodec codec = codecAt(NOW);
        String state = codec.create(1L);
        String otherUserPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("2:9999999999:abc".getBytes());

        assertThat(codec.isValid(otherUserPayload + state.substring(state.indexOf('.')), 2L)).isFalse();
        assertThat(codec.isValid("not-a-state", 1L)).isFalse();
        assertThat(codec.isValid("@@@.@@@", 1L)).isFalse();
    }
}
