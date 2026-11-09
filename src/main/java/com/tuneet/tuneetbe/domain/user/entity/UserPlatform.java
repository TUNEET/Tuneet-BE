package com.tuneet.tuneetbe.domain.user.entity;

import com.tuneet.tuneetbe.domain.user.enums.Platform;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_platforms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserPlatform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_platform_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform; // 연동한 음원 플랫폼

    private String accessToken; // 플랫폼 액세스 토큰

    private String refreshToken; // 플랫폼 리프레시 토큰

    private String externalUserId; // 플랫폼 사용자 ID

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 연동 일시
}
