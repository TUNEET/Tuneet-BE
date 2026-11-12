package com.tuneet.tuneetbe.domain.notification.repository;

import com.tuneet.tuneetbe.domain.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    // 토큰으로 조회
    Optional<DeviceToken> findByToken(String token);

    // 사용자의 모든 토큰 조회 (푸시 전송 대상)
    List<DeviceToken> findAllByUserId(Long userId);

    // 내 토큰 삭제 (로그아웃)
    void deleteByUserIdAndToken(Long userId, String token);

    // 사용자의 모든 토큰 삭제 (회원탈퇴)
    void deleteAllByUserId(Long userId);

    // 만료된 토큰 삭제
    void deleteAllByTokenIn(List<String> tokens);
}
