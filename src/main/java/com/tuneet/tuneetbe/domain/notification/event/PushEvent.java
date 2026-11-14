package com.tuneet.tuneetbe.domain.notification.event;

import com.tuneet.tuneetbe.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;

// 푸시 알림 전송 이벤트 (받는 사람, 제목, 내용, 알림 종류, 이동할 대상 ID)
// 앱 안 알림 팝업에 보여줄 보낸 사람 닉네임, 보낸 시간도 함께 전달
public record PushEvent(Long userId, String title, String body, NotificationType type, Long targetId, String senderNickname, LocalDateTime createdAt) {
}
