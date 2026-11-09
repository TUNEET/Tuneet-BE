package com.tuneet.tuneetbe.domain.user.service;

import com.tuneet.tuneetbe.domain.user.dto.request.SignUpRequest;
import com.tuneet.tuneetbe.domain.user.dto.response.EmailAvailabilityResponse;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String EMAIL = "test01@email.com";
    private static final String PASSWORD = "tuneet1234!";
    private static final String ENCODED_PASSWORD = "encoded-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        @Test
        @DisplayName("정상 요청이면 비밀번호를 암호화하고 약관 동의 일시와 함께 저장한다")
        void success() {
            // given
            SignUpRequest request = new SignUpRequest(EMAIL, PASSWORD, PASSWORD, true, true, false);
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);
            given(passwordEncoder.encode(PASSWORD)).willReturn(ENCODED_PASSWORD);

            // when
            userService.signUp(request);

            // then
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User savedUser = captor.getValue();

            assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
            assertThat(savedUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(savedUser.getTermsAgreedAt()).isNotNull();
            assertThat(savedUser.getPrivacyAgreedAt()).isNotNull();
            assertThat(savedUser.getLocationAgreedAt()).isNull();
        }

        @Test
        @DisplayName("위치정보 수집에 동의하면 동의 일시를 저장한다")
        void locationAgreed() {
            // given
            SignUpRequest request = new SignUpRequest(EMAIL, PASSWORD, PASSWORD, true, true, true);
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);
            given(passwordEncoder.encode(PASSWORD)).willReturn(ENCODED_PASSWORD);

            // when
            userService.signUp(request);

            // then
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            assertThat(captor.getValue().getLocationAgreedAt()).isNotNull();
        }

        @Test
        @DisplayName("비밀번호와 비밀번호 확인이 다르면 PASSWORD_MISMATCH 예외가 발생하고 저장하지 않는다")
        void passwordMismatch() {
            // given
            SignUpRequest request = new SignUpRequest(EMAIL, PASSWORD, "different1234!", true, true, false);

            // when & then
            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.PASSWORD_MISMATCH);

            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 EMAIL_DUPLICATE 예외가 발생하고 저장하지 않는다")
        void emailDuplicate() {
            // given
            SignUpRequest request = new SignUpRequest(EMAIL, PASSWORD, PASSWORD, true, true, false);
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.EMAIL_DUPLICATE);

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("이메일 사용 가능 여부 확인")
    class EmailAvailability {

        @Test
        @DisplayName("가입되지 않은 이메일이면 사용 가능(true)을 반환한다")
        void available() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);

            // when
            EmailAvailabilityResponse response = userService.emailAvailability(EMAIL);

            // then
            assertThat(response.getAvailable()).isTrue();
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 사용 불가(false)를 반환한다")
        void unavailable() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            // when
            EmailAvailabilityResponse response = userService.emailAvailability(EMAIL);

            // then
            assertThat(response.getAvailable()).isFalse();
        }
    }
}
