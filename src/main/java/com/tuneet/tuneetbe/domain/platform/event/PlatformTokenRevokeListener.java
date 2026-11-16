package com.tuneet.tuneetbe.domain.platform.event;

import com.tuneet.tuneetbe.domain.platform.client.GoogleOAuthClient;
import com.tuneet.tuneetbe.domain.platform.util.PlatformTokenCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformTokenRevokeListener {

    private final GoogleOAuthClient googleOAuthClient;
    private final PlatformTokenCipher cipher;

    // 삭제가 커밋된 뒤에만, 요청 스레드와 별도로 폐기 (실패해도 연동 해제는 유지)
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PlatformTokenRevokeEvent event) {
        if (event.encryptedRefreshToken() == null) {
            return;
        }
        try {
            googleOAuthClient.revoke(cipher.decrypt(event.encryptedRefreshToken()));
        } catch (IllegalStateException e) {
            log.warn("Google 토큰 폐기 건너뜀: {}", e.getMessage());
        }
    }
}
