package com.tuneet.tuneetbe.domain.notification.event;

import com.tuneet.tuneetbe.domain.notification.service.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PushEventListener {

    private final FcmService fcmService;

    // 저장 트랜잭션이 커밋된 뒤, 별도 스레드에서 푸시 전송
    @Async // 요청 스레드 말고 별도의 스레드에서 실행시키기 위해 사용
    // @TransactionalEventListener이 붙은 메서드를 찾아서 리스너가 동작한다 기억
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // AFTER_COMMIT: 편지, 메시지 저장이 커밋된 뒤에만 실행 (롤백되면 푸시를 보내지 않음)
    public void handle(PushEvent event) { // event를 받는다는 메서드
        fcmService.send(event.userId(), event.title(), event.body(), event.type(), event.targetId(), event.senderNickname(), event.createdAt());
    }
}
