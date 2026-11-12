package com.tuneet.tuneetbe.domain.notification.event;

import com.tuneet.tuneetbe.domain.notification.enums.NotificationType;

// 푸시 알림 전송 이벤트 (받는 사람, 제목, 내용, 알림 종류, 이동할 대상 ID)
public record PushEvent(Long userId, String title, String body, NotificationType type, Long targetId) {
}
