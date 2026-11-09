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

    private LocalDateTime deletedAt; // 탈퇴 일시

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

    public void updateProfile(String nickname, String profilePicture) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (profilePicture != null) {
            this.profilePicture = profilePicture;
        }
    }

    // 위치 노출과 편지 받기 변경 (null이면 유지)
    public void updateSettings(Boolean locationVisible, Boolean letterReceivable) {
        if (locationVisible != null) {
            this.locationVisible = locationVisible;
        }
        if (letterReceivable != null) {
            this.letterReceivable = letterReceivable;
        }
    }

    // 위치 동의 저장 (이미 동의했으면 기존 일시 유지)
    public void agreeLocation(LocalDateTime agreedAt) {
        if (this.locationAgreedAt == null) {
            this.locationAgreedAt = agreedAt;
            this.locationVisible = true;
        }
    }

    // 위치 동의 철회 (동의 일시와 좌표 삭제, 위치 노출 끔)
    public void withdrawLocationAgreement() {
        this.locationAgreedAt = null;
        this.latitude = null;
        this.longitude = null;
        this.locationVisible = false;
    }

    // 회원탈퇴 (개인정보를 파기하고 탈퇴 일시 저장)
    public void deleteUser(LocalDateTime deletedAt) {
        this.email = "deleted_" + this.id + "@deleted.tuneet";
        this.password = "DELETED";
        this.nickname = null;
        this.profilePicture = null;
        this.profileMusic = null;
        this.latitude = null;
        this.longitude = null;
        this.locationAgreedAt = null;
        this.locationVisible = false;
        this.letterReceivable = false;
        this.deletedAt = deletedAt;
        clearRefreshToken();
    }

    // 탈퇴한 사용자인지 확인
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
