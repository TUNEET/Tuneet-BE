package com.tuneet.tuneetbe.domain.notification.service;

import com.tuneet.tuneetbe.domain.notification.entity.DeviceToken;
import com.tuneet.tuneetbe.domain.notification.repository.DeviceTokenRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final String TOKEN = "fcm-token";

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeviceTokenService deviceTokenService;

    private User savedUser(Long id) {
        User user = User.builder()
                .email("user" + id + "@email.com")
                .password("encoded-password")
                .termsAgreedAt(LocalDateTime.now())
                .privacyAgreedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Nested
    @DisplayName("FCM 토큰 등록")
    class RegisterToken {

        @Test
        @DisplayName("처음 등록하는 토큰이면 현재 사용자의 토큰으로 저장한다")
        void newToken() {
            // given
            User me = savedUser(USER_ID);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(me));
            given(deviceTokenRepository.findByToken(TOKEN)).willReturn(Optional.empty());

            // when
            deviceTokenService.registerToken(USER_ID, TOKEN);

            // then
            ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
            verify(deviceTokenRepository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isEqualTo(me);
            assertThat(captor.getValue().getToken()).isEqualTo(TOKEN);
        }

        @Test
        @DisplayName("다른 사용자에게 등록된 토큰이면 새로 저장하지 않고 주인을 현재 사용자로 바꾼다")
        void changeUser() {
            // given
            User me = savedUser(USER_ID);
            DeviceToken existing = DeviceToken.builder().user(savedUser(OTHER_USER_ID)).token(TOKEN).build();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(me));
            given(deviceTokenRepository.findByToken(TOKEN)).willReturn(Optional.of(existing));

            // when
            deviceTokenService.registerToken(USER_ID, TOKEN);

            // then
            assertThat(existing.getUser()).isEqualTo(me);
            verify(deviceTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 USER_NOT_FOUND 예외가 발생하고 저장하지 않는다")
        void userNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deviceTokenService.registerToken(USER_ID, TOKEN))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verify(deviceTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("FCM 토큰 삭제")
    class DeleteToken {

        @Test
        @DisplayName("내 토큰만 삭제한다")
        void success() {
            // when
            deviceTokenService.deleteToken(USER_ID, TOKEN);

            // then
            verify(deviceTokenRepository).deleteByUserIdAndToken(USER_ID, TOKEN);
        }
    }
}
