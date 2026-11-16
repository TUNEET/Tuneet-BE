package com.tuneet.tuneetbe.domain.user.entity;

import com.tuneet.tuneetbe.domain.user.enums.Platform;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
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

    private String refreshToken; // 플랫폼 리프레시 토큰 (암호화, access token은 필요할 때 발급)

    private String externalUserId; // 플랫폼 사용자 ID

    @Column(length = 500)
    private String scope; // 허용받은 권한 범위

    private LocalDateTime lastSyncedAt; // 마지막 좋아요 동기화 일시

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 연동 일시

    @Builder
    private UserPlatform(User user, Platform platform, String refreshToken, String scope) {
        this.user = user;
        this.platform = platform;
        this.refreshToken = refreshToken;
        this.scope = scope;
    }

    // 다시 연동한 경우 토큰 교체 (refreshToken은 암호화된 값)
    public void updateToken(String refreshToken, String scope) {
        this.refreshToken = refreshToken;
        this.scope = scope;
    }

    public void markSynced(LocalDateTime syncedAt) {
        this.lastSyncedAt = syncedAt;
    }
}
