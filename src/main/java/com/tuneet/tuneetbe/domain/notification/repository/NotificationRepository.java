package com.tuneet.tuneetbe.domain.notification.repository;

import com.tuneet.tuneetbe.domain.notification.entity.Notification;
import com.tuneet.tuneetbe.domain.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 사용자가 받은 알림 전체 삭제
    void deleteAllByReceiverId(Long receiverId);

    // 알림 종류와 대상 ID 목록에 해당하는 알림 삭제
    void deleteAllByTypeAndTargetIdIn(NotificationType type, List<Long> targetIds);
}
