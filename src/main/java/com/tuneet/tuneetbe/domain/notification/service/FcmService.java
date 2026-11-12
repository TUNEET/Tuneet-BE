package com.tuneet.tuneetbe.domain.notification.service;

import com.google.firebase.messaging.*;
import com.tuneet.tuneetbe.domain.notification.entity.DeviceToken;
import com.tuneet.tuneetbe.domain.notification.enums.NotificationType;
import com.tuneet.tuneetbe.domain.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private final ObjectProvider<FirebaseMessaging> firebaseMessaging;
    private final DeviceTokenRepository deviceTokenRepository;

    // 사용자의 모든 기기로 푸시 전송
    @Transactional
    public void send(Long userId, String title, String body, NotificationType type, Long targetId) {

        // Firebase 키가 없는 환경이면 전송하지 않음
        FirebaseMessaging messaging = firebaseMessaging.getIfAvailable();
        if (messaging == null) {
            return;
        }

        // 받는 사람의 기기 토큰 조회
        List<String> tokens = new ArrayList<>();
        for (DeviceToken deviceToken : deviceTokenRepository.findAllByUserId(userId)) {
            tokens.add(deviceToken.getToken());
        }

        // 등록된 기기가 없으면 전송하지 않음
        if (tokens.isEmpty()) {
            return;
        }

        // 푸시 메시지 세팅 (알림을 누르면 이동할 화면 정보를 data에 담음)
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens) // 받는 기기 목록
                .setNotification(Notification.builder()
                        .setTitle(title) // 알림 제목
                        .setBody(body) // 알림 내용
                        .build())
                .putData("type", type.name()) // 알림 종류
                .putData("targetId", String.valueOf(targetId)) // 이동할 대상 ID
                .build();

        try {
            // 푸시 전송
            BatchResponse response = messaging.sendEachForMulticast(message);

            // 만료되거나 앱이 삭제된 기기의 토큰 찾기
            List<String> invalidTokens = new ArrayList<>();
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse sendResponse = responses.get(i);
                if (!sendResponse.isSuccessful()
                        && sendResponse.getException().getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    invalidTokens.add(tokens.get(i));
                }
            }

            // 만료된 토큰 삭제
            if (!invalidTokens.isEmpty()) {
                deviceTokenRepository.deleteAllByTokenIn(invalidTokens);
            }

            // 로그 출력
            log.info("[FcmService] 푸시 전송 완료: userId = {}, success = {}, failure = {}", userId, response.getSuccessCount(), response.getFailureCount());
        }
        catch (FirebaseMessagingException e) {
            // 푸시 실패는 편지, 메시지 저장에 영향을 주지 않도록 로그만 남김
            log.error("[FcmService] 푸시 전송 실패: userId = {}", userId, e);
        }
    }
}
