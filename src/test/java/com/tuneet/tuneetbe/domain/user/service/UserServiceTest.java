package com.tuneet.tuneetbe.domain.user.service;

import com.tuneet.tuneetbe.domain.user.dto.request.ProfileAddRequest;
import com.tuneet.tuneetbe.domain.user.dto.request.SignUpRequest;
import com.tuneet.tuneetbe.domain.user.dto.response.EmailAvailabilityResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.NicknameAvailabilityResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.ProfileAddResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "test01@email.com";
    private static final String PASSWORD = "tuneet1234!";
    private static final String ENCODED_PASSWORD = "encoded-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User savedUser(String nickname, String profilePicture) {
        User user = User.builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .termsAgreedAt(LocalDateTime.now())
                .privacyAgreedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.updateProfile(nickname, profilePicture);
        return user;
    }

    @Nested
    @DisplayName("프로필 추가 설정")
    class ProfileAdd {

        @Test
        @DisplayName("닉네임과 프로필 사진을 로그인한 사용자에게 저장한다")
        void success() {
            // given
            User user = savedUser(null, null);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userRepository.existsByNickname("튜니")).willReturn(false);

            // when
            ProfileAddResponse response = userService.profileAdd(USER_ID, new ProfileAddRequest("튜니", "new.png"));

            // then
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getNickname()).isEqualTo("튜니");
            assertThat(response.getProfilePicture()).isEqualTo("new.png");
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            verify(userRepository).saveAndFlush(user);
        }

        @Test
        @DisplayName("프로필 사진만 보내면 닉네임은 유지하고 중복 검사를 하지 않는다")
        void pictureOnly() {
            // given
            User user = savedUser("튜니", "old.png");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            userService.profileAdd(USER_ID, new ProfileAddRequest(null, "new.png"));

            // then
            assertThat(user.getNickname()).isEqualTo("튜니");
            assertThat(user.getProfilePicture()).isEqualTo("new.png");
            verify(userRepository, never()).existsByNickname(any());
        }

        @Test
        @DisplayName("닉네임만 보내면 프로필 사진은 유지한다")
        void nicknameOnly() {
            // given
            User user = savedUser("튜니", "old.png");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userRepository.existsByNickname("새닉네임")).willReturn(false);

            // when
            userService.profileAdd(USER_ID, new ProfileAddRequest("새닉네임", null));

            // then
            assertThat(user.getNickname()).isEqualTo("새닉네임");
            assertThat(user.getProfilePicture()).isEqualTo("old.png");
        }

        @Test
        @DisplayName("현재 닉네임을 그대로 보내면 중복 검사를 하지 않고 통과한다")
        void sameNickname() {
            // given
            User user = savedUser("튜니", "old.png");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            userService.profileAdd(USER_ID, new ProfileAddRequest("튜니", null));

            // then
            assertThat(user.getNickname()).isEqualTo("튜니");
            verify(userRepository, never()).existsByNickname(any());
        }

        @Test
        @DisplayName("다른 사용자의 닉네임이면 NICKNAME_DUPLICATE 예외가 발생하고 변경하지 않는다")
        void duplicateNickname() {
            // given
            User user = savedUser("튜니", "old.png");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userRepository.existsByNickname("원영")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.profileAdd(USER_ID, new ProfileAddRequest("원영", "new.png")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.NICKNAME_DUPLICATE);

            assertThat(user.getNickname()).isEqualTo("튜니");
            assertThat(user.getProfilePicture()).isEqualTo("old.png");
            verify(userRepository, never()).saveAndFlush(any(User.class));
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void userNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.profileAdd(USER_ID, new ProfileAddRequest("튜니", null)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("닉네임 사용 가능 여부 확인")
    class NicknameAvailability {

        @Test
        @DisplayName("사용 중이 아닌 닉네임이면 사용 가능(true)을 반환한다")
        void available() {
            // given
            given(userRepository.existsByNickname("튜니")).willReturn(false);

            // when
            NicknameAvailabilityResponse response = userService.nicknameAvailability("튜니");

            // then
            assertThat(response.getAvailable()).isTrue();
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 사용 불가(false)를 반환한다")
        void unavailable() {
            // given
            given(userRepository.existsByNickname("튜니")).willReturn(true);

            // when
            NicknameAvailabilityResponse response = userService.nicknameAvailability("튜니");

            // then
            assertThat(response.getAvailable()).isFalse();
        }
    }

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
