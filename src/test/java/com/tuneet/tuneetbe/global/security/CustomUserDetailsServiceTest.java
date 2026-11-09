package com.tuneet.tuneetbe.global.security;

import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User savedUser() {
        User user = User.builder()
                .email("test01@email.com")
                .password("encoded-password")
                .termsAgreedAt(LocalDateTime.now())
                .privacyAgreedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    @Test
    @DisplayName("탈퇴하지 않은 사용자는 인증 정보를 반환한다")
    void activeUser() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(savedUser()));

        // when
        CustomUserDetails userDetails = customUserDetailsService.loadUserById(USER_ID);

        // then
        assertThat(userDetails.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("탈퇴한 사용자의 토큰은 인증하지 않는다")
    void deletedUser() {
        // given
        User user = savedUser();
        user.deleteUser(LocalDateTime.now());
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> customUserDetailsService.loadUserById(USER_ID))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
