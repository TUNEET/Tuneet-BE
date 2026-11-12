package com.tuneet.tuneetbe.domain.notification.entity;

import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "device_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 토큰을 등록한 사용자

    @Column(nullable = false, unique = true, length = 500)
    private String token; // FCM 기기 토큰

    @Builder
    private DeviceToken(User user, String token) {
        this.user = user;
        this.token = token;
    }

    // 같은 기기에서 다른 계정으로 로그인하면 토큰의 주인을 변경
    public void changeUser(User user) {
        this.user = user;
    }
}
