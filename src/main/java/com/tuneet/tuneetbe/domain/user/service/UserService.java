package com.tuneet.tuneetbe.domain.user.service;

import com.tuneet.tuneetbe.domain.user.dto.request.SignUpRequest;
import com.tuneet.tuneetbe.domain.user.dto.response.EmailAvailabilityResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.SignUpResponse;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {

        // 비밀번호가 일치하는지 조회
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            log.warn("[UserService] 비밀번호가 일치하지 않습니다.");
            throw new CustomException(UserErrorCode.PASSWORD_MISMATCH);
        }

        // 중복된 이메일인지 조회
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("[UserService] 중복된 이메일입니다.");
            throw new CustomException(UserErrorCode.EMAIL_DUPLICATE);
        }

        LocalDateTime now = LocalDateTime.now();

        // 회원가입 저장
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .termsAgreedAt(now)
                .privacyAgreedAt(now)
                .locationAgreedAt(Boolean.TRUE.equals(request.getLocationAgreed()) ? now : null)
                .build();

        // DB에 저장
        userRepository.save(user);

        // 로그 출력
        log.info("[UserService] 회원가입 성공: userId = {}", user.getId());

        // 응답 반환
        return SignUpResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // 이메일 사용 가능 여부 확인
    public EmailAvailabilityResponse emailAvailability(String email) {

        // 가입된 이메일인지 조회
        boolean existsByEmail = userRepository.existsByEmail(email);

        // 가입되지 않은 이메일이면 사용 가능
        boolean available = !existsByEmail;

        // 응답 반환
        return EmailAvailabilityResponse.builder()
                .available(available)
                .build();
    }
}
