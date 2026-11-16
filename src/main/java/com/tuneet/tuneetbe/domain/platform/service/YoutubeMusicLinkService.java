package com.tuneet.tuneetbe.domain.platform.service;

import com.tuneet.tuneetbe.domain.platform.client.GoogleOAuthClient;
import com.tuneet.tuneetbe.domain.platform.client.GoogleOAuthClient.TokenResponse;
import com.tuneet.tuneetbe.domain.platform.dto.request.PlatformLinkRequest;
import com.tuneet.tuneetbe.domain.platform.dto.response.PlatformAuthorizeUrlResponse;
import com.tuneet.tuneetbe.domain.platform.event.PlatformTokenRevokeEvent;
import com.tuneet.tuneetbe.domain.platform.exception.PlatformErrorCode;
import com.tuneet.tuneetbe.domain.platform.util.OAuthStateCodec;
import com.tuneet.tuneetbe.domain.platform.util.PlatformTokenCipher;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.entity.UserPlatform;
import com.tuneet.tuneetbe.domain.user.enums.LikeSource;
import com.tuneet.tuneetbe.domain.user.enums.Platform;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserMusicLikeRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserPlatformRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 유튜브 뮤직 연동과 연동 해제.
 * Google 호출은 트랜잭션 밖에서 하고, DB 저장만 짧은 트랜잭션으로 묶는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeMusicLinkService {

    private final UserRepository userRepository;
    private final UserPlatformRepository userPlatformRepository;
    private final UserMusicLikeRepository userMusicLikeRepository;
    private final GoogleOAuthClient googleOAuthClient;
    private final OAuthStateCodec stateCodec;
    private final PlatformTokenCipher cipher;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

    // 연동 URL 발급
    public PlatformAuthorizeUrlResponse getAuthorizeUrl(Long userId) {
        return PlatformAuthorizeUrlResponse.builder()
                .url(googleOAuthClient.authorizeUrl(stateCodec.create(userId)))
                .build();
    }

    // 연동 (인가 코드를 토큰으로 교환해 refresh token 저장)
    public void link(Long userId, PlatformLinkRequest request) {

        // 이 사용자가 발급받은 state인지 확인 (다른 계정에 연결되는 것 방지)
        if (!stateCodec.isValid(request.getState(), userId)) {
            throw new CustomException(PlatformErrorCode.PLATFORM_INVALID_STATE);
        }

        TokenResponse token = googleOAuthClient.exchangeCode(request.getCode());

        // 동의 화면에서 권한을 해제하고 동의한 경우
        if (!token.hasScope(GoogleOAuthClient.YOUTUBE_READONLY_SCOPE)) {
            throw new CustomException(PlatformErrorCode.PLATFORM_SCOPE_DENIED);
        }
        if (token.refreshToken() == null) {
            log.warn("[YoutubeMusicLinkService] refresh token 없음: userId = {}", userId);
            throw new CustomException(PlatformErrorCode.PLATFORM_AUTH_FAILED);
        }
        String encrypted = cipher.encrypt(token.refreshToken());

        transactionTemplate.executeWithoutResult(status -> {
            User user = findUserForUpdate(userId);
            userPlatformRepository.findByUserIdAndPlatform(userId, Platform.YOUTUBE_MUSIC)
                    .ifPresentOrElse(
                            platform -> platform.updateToken(encrypted, token.scope()),
                            () -> userPlatformRepository.save(UserPlatform.builder()
                                    .user(user)
                                    .platform(Platform.YOUTUBE_MUSIC)
                                    .refreshToken(encrypted)
                                    .scope(token.scope())
                                    .build()));
        });
        log.info("[YoutubeMusicLinkService] 유튜브 뮤직 연동 성공: userId = {}", userId);
    }

    // 연동 해제 (불러온 좋아요 삭제, 커밋 후 Google 토큰 폐기)
    public void unlink(Long userId) {
        transactionTemplate.executeWithoutResult(status -> {
            findUserForUpdate(userId);
            UserPlatform platform = userPlatformRepository.findByUserIdAndPlatform(userId, Platform.YOUTUBE_MUSIC)
                    .orElseThrow(() -> new CustomException(PlatformErrorCode.PLATFORM_NOT_CONNECTED));
            userMusicLikeRepository.deleteAllByUserIdAndSource(userId, LikeSource.YOUTUBE_MUSIC);
            userPlatformRepository.delete(platform);
            eventPublisher.publishEvent(new PlatformTokenRevokeEvent(platform.getRefreshToken()));
        });
        log.info("[YoutubeMusicLinkService] 유튜브 뮤직 연동 해제 성공: userId = {}", userId);
    }

    // 같은 사용자의 연동·해제·동기화가 순서대로 처리되도록 사용자 행을 잠금
    private User findUserForUpdate(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        if (user.isDeleted()) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
