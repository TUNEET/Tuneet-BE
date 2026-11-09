package com.tuneet.tuneetbe.domain.auth.service;

import com.tuneet.tuneetbe.domain.auth.dto.reqeust.LoginRequest;
import com.tuneet.tuneetbe.domain.auth.dto.reqeust.ReissueRequest;
import com.tuneet.tuneetbe.domain.auth.dto.response.LoginResponse;
import com.tuneet.tuneetbe.domain.auth.exception.AuthErrorCode;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import com.tuneet.tuneetbe.global.security.CustomUserDetails;
import com.tuneet.tuneetbe.global.security.CustomUserDetailsService;
import com.tuneet.tuneetbe.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    // 로그인
    @Transactional
    public LoginResponse login(LoginRequest request) {

        // email로 사용자 조회
        CustomUserDetails userDetails;
        try {
            userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(request.getEmail());
        } catch (UsernameNotFoundException e) {
            throw new CustomException(AuthErrorCode.PASSWORD_FAIL);
        }

        // 입력한 비밀번호와 DB에 저장된 암호화 비밀번호 비교
        if (!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
            throw new CustomException(AuthErrorCode.PASSWORD_FAIL);
        }

        // 로그인 성공 시 Access Token 발급
        String accessToken = jwtProvider.createAccessToken(userDetails);

        // Refresh Token 발급 및 저장
        String refreshToken = issueRefreshToken(userDetails.getUser());

        // AccessToken을 응답으로 반환
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 토큰 재발급
    @Transactional
    public LoginResponse reissue(ReissueRequest request) {

        // Refresh Token 서명, 만료 시간, 타입 검증
        if (!jwtProvider.validateRefreshToken(request.getRefreshToken())) {
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 토큰에서 userId를 추출해 사용자 조회
        User user = userRepository.findById(jwtProvider.getUserId(request.getRefreshToken()))
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        // 저장된 Refresh Token과 일치하는지, 만료되지 않았는지 확인
        if (user.getRefreshTokenHash() == null
                || !MessageDigest.isEqual(
                        user.getRefreshTokenHash().getBytes(StandardCharsets.UTF_8),
                        hash(request.getRefreshToken()).getBytes(StandardCharsets.UTF_8))
                || user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // Access Token 재발급 및 Refresh Token 교체
        String accessToken = jwtProvider.createAccessToken(new CustomUserDetails(user));
        String refreshToken = issueRefreshToken(user);

        // 응답 반환
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 로그아웃
    @Transactional
    public void logout(Long userId) {

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 저장된 Refresh Token 삭제
        user.clearRefreshToken();
    }

    private String issueRefreshToken(User user) {
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        LocalDateTime expiresAt = LocalDateTime.now().plus(Duration.ofMillis(jwtProvider.getRefreshTokenExpiration()));
        user.updateRefreshToken(hash(refreshToken), expiresAt);
        return refreshToken;
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
