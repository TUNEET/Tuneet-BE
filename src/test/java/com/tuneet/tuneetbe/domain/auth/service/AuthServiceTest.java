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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "test01@email.com";
    private static final String PASSWORD = "tuneet1234!";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String OLD_REFRESH_TOKEN = "old-refresh-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private static final long REFRESH_EXPIRATION = 1209600000L;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .termsAgreedAt(LocalDateTime.now())
                .privacyAgreedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", EMAIL);
        ReflectionTestUtils.setField(request, "password", PASSWORD);
        return request;
    }

    private ReissueRequest reissueRequest(String refreshToken) {
        ReissueRequest request = new ReissueRequest();
        ReflectionTestUtils.setField(request, "refreshToken", refreshToken);
        return request;
    }

    private String hash(String token) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    private void assertErrorCode(Runnable action, Object errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("정상 요청이면 토큰 2개를 발급하고 Refresh Token 해시를 저장한다")
        void success() throws Exception {
            // given
            given(customUserDetailsService.loadUserByUsername(EMAIL)).willReturn(new CustomUserDetails(user));
            given(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            given(jwtProvider.createAccessToken(any(CustomUserDetails.class))).willReturn(ACCESS_TOKEN);
            given(jwtProvider.createRefreshToken(USER_ID)).willReturn(NEW_REFRESH_TOKEN);
            given(jwtProvider.getRefreshTokenExpiration()).willReturn(REFRESH_EXPIRATION);

            // when
            LoginResponse response = authService.login(loginRequest());

            // then
            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
            assertThat(user.getRefreshTokenHash()).isEqualTo(hash(NEW_REFRESH_TOKEN));
            assertThat(user.getRefreshTokenHash()).isNotEqualTo(NEW_REFRESH_TOKEN);
            assertThat(user.getRefreshTokenExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("비밀번호가 틀리면 PASSWORD_FAIL 예외가 발생하고 토큰을 발급하지 않는다")
        void wrongPassword() {
            // given
            given(customUserDetailsService.loadUserByUsername(EMAIL)).willReturn(new CustomUserDetails(user));
            given(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).willReturn(false);

            // when & then
            assertErrorCode(() -> authService.login(loginRequest()), AuthErrorCode.PASSWORD_FAIL);
            verify(jwtProvider, never()).createAccessToken(any(CustomUserDetails.class));
            assertThat(user.getRefreshTokenHash()).isNull();
        }

        @Test
        @DisplayName("없는 이메일이면 비밀번호 오류와 같은 PASSWORD_FAIL 예외가 발생한다")
        void emailNotFound() {
            // given
            given(customUserDetailsService.loadUserByUsername(EMAIL))
                    .willThrow(new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

            // when & then
            assertErrorCode(() -> authService.login(loginRequest()), AuthErrorCode.PASSWORD_FAIL);
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Reissue {

        private void givenValidToken() {
            given(jwtProvider.validateRefreshToken(OLD_REFRESH_TOKEN)).willReturn(true);
            given(jwtProvider.getUserId(OLD_REFRESH_TOKEN)).willReturn(USER_ID);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        }

        @Test
        @DisplayName("저장된 Refresh Token과 일치하면 토큰 2개를 새로 발급하고 해시를 교체한다")
        void success() throws Exception {
            // given
            givenValidToken();
            user.updateRefreshToken(hash(OLD_REFRESH_TOKEN), LocalDateTime.now().plusDays(1));
            given(jwtProvider.createAccessToken(any(CustomUserDetails.class))).willReturn(ACCESS_TOKEN);
            given(jwtProvider.createRefreshToken(USER_ID)).willReturn(NEW_REFRESH_TOKEN);
            given(jwtProvider.getRefreshTokenExpiration()).willReturn(REFRESH_EXPIRATION);

            // when
            LoginResponse response = authService.reissue(reissueRequest(OLD_REFRESH_TOKEN));

            // then
            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
            assertThat(user.getRefreshTokenHash()).isEqualTo(hash(NEW_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("JWT 검증에 실패하면 INVALID_REFRESH_TOKEN 예외가 발생하고 사용자를 조회하지 않는다")
        void invalidJwt() {
            // given
            given(jwtProvider.validateRefreshToken(OLD_REFRESH_TOKEN)).willReturn(false);

            // when & then
            assertErrorCode(() -> authService.reissue(reissueRequest(OLD_REFRESH_TOKEN)), AuthErrorCode.INVALID_REFRESH_TOKEN);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("사용자가 없으면 INVALID_REFRESH_TOKEN 예외가 발생한다")
        void userNotFound() {
            // given
            given(jwtProvider.validateRefreshToken(OLD_REFRESH_TOKEN)).willReturn(true);
            given(jwtProvider.getUserId(OLD_REFRESH_TOKEN)).willReturn(USER_ID);
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertErrorCode(() -> authService.reissue(reissueRequest(OLD_REFRESH_TOKEN)), AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("로그아웃해서 저장된 값이 없으면 INVALID_REFRESH_TOKEN 예외가 발생한다")
        void loggedOut() {
            // given
            givenValidToken();

            // when & then
            assertErrorCode(() -> authService.reissue(reissueRequest(OLD_REFRESH_TOKEN)), AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("저장된 값과 다른 토큰이면 INVALID_REFRESH_TOKEN 예외가 발생한다")
        void hashMismatch() throws Exception {
            // given
            givenValidToken();
            user.updateRefreshToken(hash("other-refresh-token"), LocalDateTime.now().plusDays(1));

            // when & then
            assertErrorCode(() -> authService.reissue(reissueRequest(OLD_REFRESH_TOKEN)), AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("저장된 만료 일시가 지났으면 INVALID_REFRESH_TOKEN 예외가 발생한다")
        void expired() throws Exception {
            // given
            givenValidToken();
            user.updateRefreshToken(hash(OLD_REFRESH_TOKEN), LocalDateTime.now().minusSeconds(1));

            // when & then
            assertErrorCode(() -> authService.reissue(reissueRequest(OLD_REFRESH_TOKEN)), AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("저장된 Refresh Token 값을 비운다")
        void success() {
            // given
            user.updateRefreshToken("saved-hash", LocalDateTime.now().plusDays(1));
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            authService.logout(USER_ID);

            // then
            assertThat(user.getRefreshTokenHash()).isNull();
            assertThat(user.getRefreshTokenExpiresAt()).isNull();
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void userNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertErrorCode(() -> authService.logout(USER_ID), UserErrorCode.USER_NOT_FOUND);
        }
    }
}
