package com.tuneet.tuneetbe.domain.platform.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 플랫폼 refresh token 암호화(AES-GCM)와 연동 state 서명(HMAC-SHA256).
 * 키는 PLATFORM_TOKEN_KEY(32바이트 Base64) 하나에서 용도별로 나눠 쓴다.
 */
@Component
public class PlatformTokenCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec encryptionKey;
    private final SecretKeySpec signingKey;

    public PlatformTokenCipher(@Value("${platform.token-key:}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            // 키 없이도 서버는 뜨고, 연동 기능을 쓸 때 실패
            this.encryptionKey = null;
            this.signingKey = null;
            return;
        }
        byte[] key = Base64.getDecoder().decode(base64Key.trim());
        if (key.length != 32) {
            throw new IllegalStateException("platform.token-key는 32바이트 Base64여야 합니다.");
        }
        this.encryptionKey = new SecretKeySpec(derive(key, "encrypt"), "AES");
        this.signingKey = new SecretKeySpec(derive(key, "state"), "HmacSHA256");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, requireKey(encryptionKey), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("토큰 암호화 실패", e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            byte[] data = Base64.getDecoder().decode(cipherText);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, requireKey(encryptionKey), new GCMParameterSpec(TAG_BITS, data, 0, IV_LENGTH));
            return new String(cipher.doFinal(data, IV_LENGTH, data.length - IV_LENGTH), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("토큰 복호화 실패", e);
        }
    }

    public byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(requireKey(signingKey));
            return mac.doFinal(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("state 서명 실패", e);
        }
    }

    public boolean verify(byte[] payload, byte[] signature) {
        return MessageDigest.isEqual(sign(payload), signature);
    }

    public byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    private static byte[] derive(byte[] key, String purpose) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(purpose.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static SecretKeySpec requireKey(SecretKeySpec key) {
        if (key == null) {
            throw new IllegalStateException("platform.token-key가 설정되지 않았습니다.");
        }
        return key;
    }
}
