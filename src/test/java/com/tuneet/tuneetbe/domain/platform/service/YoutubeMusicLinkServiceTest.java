package com.tuneet.tuneetbe.domain.platform.service;

import com.tuneet.tuneetbe.domain.platform.client.GoogleOAuthClient;
import com.tuneet.tuneetbe.domain.platform.client.GoogleOAuthClient.TokenResponse;
import com.tuneet.tuneetbe.domain.platform.dto.request.PlatformLinkRequest;
import com.tuneet.tuneetbe.domain.platform.event.PlatformTokenRevokeEvent;
import com.tuneet.tuneetbe.domain.platform.exception.PlatformErrorCode;
import com.tuneet.tuneetbe.domain.platform.util.OAuthStateCodec;
import com.tuneet.tuneetbe.domain.platform.util.PlatformTokenCipher;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.entity.UserPlatform;
import com.tuneet.tuneetbe.domain.user.enums.LikeSource;
import com.tuneet.tuneetbe.domain.user.enums.Platform;
import com.tuneet.tuneetbe.domain.user.repository.UserMusicLikeRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserPlatformRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class YoutubeMusicLinkServiceTest {

    private static final Long USER_ID = 1L;
    private static final String SCOPE = GoogleOAuthClient.YOUTUBE_READONLY_SCOPE;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPlatformRepository userPlatformRepository;

    @Mock
    private UserMusicLikeRepository userMusicLikeRepository;

    @Mock
    private GoogleOAuthClient googleOAuthClient;

    @Mock
    private OAuthStateCodec stateCodec;

    @Mock
    private PlatformTokenCipher cipher;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private YoutubeMusicLinkService service;

    @BeforeEach
    void setUp() {
        service = new YoutubeMusicLinkService(userRepository, userPlatformRepository, userMusicLikeRepository,
                googleOAuthClient, stateCodec, cipher, new TransactionTemplate(transactionManager), eventPublisher);
    }

    private User user() {
        return BeanUtils.instantiateClass(User.class);
    }

    private UserPlatform platform(String encryptedToken) {
        return UserPlatform.builder().user(user()).platform(Platform.YOUTUBE_MUSIC)
                .refreshToken(encryptedToken).scope(SCOPE).build();
    }

    @Test
    @DisplayName("연동 URL은 로그인한 사용자의 state를 담는다")
    void authorizeUrl() {
        given(stateCodec.create(USER_ID)).willReturn("state");
        given(googleOAuthClient.authorizeUrl("state")).willReturn("https://accounts.google.com/...state=state");

        assertThat(service.getAuthorizeUrl(USER_ID).getUrl()).endsWith("state=state");
    }

    @Test
    @DisplayName("다른 사용자의 state나 만료된 state면 코드를 교환하지 않는다")
    void invalidState() {
        given(stateCodec.isValid("state", USER_ID)).willReturn(false);

        assertThatThrownBy(() -> service.link(USER_ID, new PlatformLinkRequest("code", "state")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlatformErrorCode.PLATFORM_INVALID_STATE));
        verify(googleOAuthClient, never()).exchangeCode(anyString());
    }

    @Test
    @DisplayName("유튜브 읽기 권한을 허용하지 않았으면 저장하지 않는다")
    void scopeDenied() {
        given(stateCodec.isValid("state", USER_ID)).willReturn(true);
        given(googleOAuthClient.exchangeCode("code")).willReturn(new TokenResponse("access", "refresh", 3599L, "openid"));

        assertThatThrownBy(() -> service.link(USER_ID, new PlatformLinkRequest("code", "state")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlatformErrorCode.PLATFORM_SCOPE_DENIED));
        verify(userPlatformRepository, never()).save(any());
    }

    @Test
    @DisplayName("처음 연동하면 암호화한 refresh token을 저장한다")
    void linkNew() {
        given(stateCodec.isValid("state", USER_ID)).willReturn(true);
        given(googleOAuthClient.exchangeCode("code")).willReturn(new TokenResponse("access", "refresh", 3599L, SCOPE));
        given(cipher.encrypt("refresh")).willReturn("encrypted");
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user()));
        given(userPlatformRepository.findByUserIdAndPlatform(USER_ID, Platform.YOUTUBE_MUSIC)).willReturn(Optional.empty());

        service.link(USER_ID, new PlatformLinkRequest("code", "state"));

        ArgumentCaptor<UserPlatform> saved = ArgumentCaptor.forClass(UserPlatform.class);
        verify(userPlatformRepository).save(saved.capture());
        assertThat(saved.getValue().getRefreshToken()).isEqualTo("encrypted");
        assertThat(saved.getValue().getPlatform()).isEqualTo(Platform.YOUTUBE_MUSIC);
    }

    @Test
    @DisplayName("이미 연동되어 있으면 토큰만 교체한다")
    void relink() {
        UserPlatform existing = platform("old");
        given(stateCodec.isValid("state", USER_ID)).willReturn(true);
        given(googleOAuthClient.exchangeCode("code")).willReturn(new TokenResponse("access", "refresh", 3599L, SCOPE));
        given(cipher.encrypt("refresh")).willReturn("new");
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user()));
        given(userPlatformRepository.findByUserIdAndPlatform(USER_ID, Platform.YOUTUBE_MUSIC)).willReturn(Optional.of(existing));

        service.link(USER_ID, new PlatformLinkRequest("code", "state"));

        assertThat(existing.getRefreshToken()).isEqualTo("new");
        verify(userPlatformRepository, never()).save(any());
    }

    @Test
    @DisplayName("연동 해제는 불러온 좋아요를 지우고 토큰 폐기 이벤트를 발행한다")
    void unlink() {
        UserPlatform existing = platform("encrypted");
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user()));
        given(userPlatformRepository.findByUserIdAndPlatform(USER_ID, Platform.YOUTUBE_MUSIC)).willReturn(Optional.of(existing));

        service.unlink(USER_ID);

        verify(userMusicLikeRepository).deleteAllByUserIdAndSource(USER_ID, LikeSource.YOUTUBE_MUSIC);
        verify(userPlatformRepository).delete(existing);
        verify(eventPublisher).publishEvent(new PlatformTokenRevokeEvent("encrypted"));
    }

    @Test
    @DisplayName("연동하지 않았으면 해제할 수 없다")
    void unlinkNotConnected() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user()));
        given(userPlatformRepository.findByUserIdAndPlatform(USER_ID, Platform.YOUTUBE_MUSIC)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.unlink(USER_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlatformErrorCode.PLATFORM_NOT_CONNECTED));
        verify(userMusicLikeRepository, never()).deleteAllByUserIdAndSource(any(), any());
    }
}
