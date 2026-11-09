package com.tuneet.tuneetbe.domain.notification.repository;

import com.tuneet.tuneetbe.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 사용자가 받은 알림 전체 삭제
    void deleteAllByReceiverId(Long receiverId);
}
