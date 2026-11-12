package com.tuneet.tuneetbe.domain.notification.service;

import com.tuneet.tuneetbe.domain.notification.entity.DeviceToken;
import com.tuneet.tuneetbe.domain.notification.repository.DeviceTokenRepository;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    // FCM 토큰 등록
    @Transactional
    public void registerToken(Long userId, String token) {

        // 로그인한 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 이미 있는 토큰이면 주인만 현재 사용자로 변경 (같은 기기에서 다른 계정 로그인할 때)
        Optional<DeviceToken> existing = deviceTokenRepository.findByToken(token);
        if (existing.isPresent()) {
            existing.get().changeUser(user);
            log.info("[DeviceTokenService] FCM 토큰 사용자 변경: userId = {}", userId);
            return;
        }

        // DB 저장
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user)
                .token(token)
                .build());

        // 로그 출력
        log.info("[DeviceTokenService] FCM 토큰 등록 성공: userId = {}", userId);
    }

    // FCM 토큰 삭제 (로그아웃 전에 호출)
    @Transactional
    public void deleteToken(Long userId, String token) {

        // 내 토큰만 삭제
        deviceTokenRepository.deleteByUserIdAndToken(userId, token);

        // 로그 출력
        log.info("[DeviceTokenService] FCM 토큰 삭제 성공: userId = {}", userId);
    }
}
