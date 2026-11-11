package com.tuneet.tuneetbe.domain.notification.repository;

import com.tuneet.tuneetbe.domain.notification.entity.Notification;
import com.tuneet.tuneetbe.domain.notification.enums.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 사용자가 받은 알림 전체 삭제
    void deleteAllByReceiverId(Long receiverId);

    // 알림 종류와 대상 ID 목록에 해당하는 알림 삭제
    void deleteAllByTypeAndTargetIdIn(NotificationType type, List<Long> targetIds);

    // 내가 받은 알림을 최신순으로 보낸 사람과 함께 조회
    @EntityGraph(attributePaths = "sender")
    Slice<Notification> findByReceiverIdOrderByIdDesc(Long receiverId, Pageable pageable);

    // 내가 받은 알림 중 커서보다 이전 알림을 보낸 사람과 함께 조회
    @EntityGraph(attributePaths = "sender")
    Slice<Notification> findByReceiverIdAndIdLessThanOrderByIdDesc(Long receiverId, Long cursor, Pageable pageable);
}
