package com.tuneet.tuneetbe.domain.notification.entity;

import com.tuneet.tuneetbe.domain.notification.enums.NotificationType;
import com.tuneet.tuneetbe.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver; // 알림 받는 사람

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender; // 알림 보낸 사람

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type; // 알림 종류

    @Column(nullable = false)
    private Long targetId; // 알림 대상 ID (type에 따라 편지 / 피드 코멘트 / 피드 반응 ID)

    @Column(nullable = false)
    private Boolean isRead = false; // 읽음 여부

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 알림 일시
}
