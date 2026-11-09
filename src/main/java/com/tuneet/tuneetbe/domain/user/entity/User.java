package com.tuneet.tuneetbe.domain.user.entity;

import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_music_id")
    private Music profileMusic; // 프로필 음악 (나를 닮은 음악)

    @Column(nullable = false, unique = true)
    private String email; // 이메일

    @Column(nullable = false)
    private String password; // 비밀번호

    @Column(unique = true)
    private String nickname; // 닉네임

    private String profilePicture; // 프로필 사진

    private Double latitude; // 위도

    private Double longitude; // 경도

    @Column(nullable = false)
    private LocalDateTime termsAgreedAt; // 서비스 이용약관 동의 일시

    @Column(nullable = false)
    private LocalDateTime privacyAgreedAt; // 개인정보 처리방침 동의 일시

    private LocalDateTime locationAgreedAt; // 위치정보 수집 및 이용 동의 일시 (선택)

    @Column(nullable = false)
    private Boolean locationVisible = true; // 위치정보 노출 여부

    @Column(nullable = false)
    private Boolean letterReceivable = true; // 편지 받기 여부

    private String refreshTokenHash; // Refresh Token 해시값

    private LocalDateTime refreshTokenExpiresAt; // Refresh Token 만료 일시

    @Builder
    private User(String email, String password, LocalDateTime termsAgreedAt, LocalDateTime privacyAgreedAt, LocalDateTime locationAgreedAt) {
        this.email = email;
        this.password = password;
        this.termsAgreedAt = termsAgreedAt;
        this.privacyAgreedAt = privacyAgreedAt;
        this.locationAgreedAt = locationAgreedAt;
    }

    public void updateRefreshToken(String refreshTokenHash, LocalDateTime refreshTokenExpiresAt) {
        this.refreshTokenHash = refreshTokenHash;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    public void clearRefreshToken() {
        this.refreshTokenHash = null;
        this.refreshTokenExpiresAt = null;
    }
}
