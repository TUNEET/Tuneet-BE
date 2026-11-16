package com.tuneet.tuneetbe.domain.platform.scheduler;

import com.tuneet.tuneetbe.domain.platform.service.YoutubeMusicSyncService;
import com.tuneet.tuneetbe.domain.user.enums.Platform;
import com.tuneet.tuneetbe.domain.user.repository.UserPlatformRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

// YouTube 정책상 저장한 데이터는 30일 안에 갱신해야 하므로 25일이 지난 연동을 매일 다시 불러옴
@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeMusicResyncScheduler {

    static final int RESYNC_AFTER_DAYS = 25;

    private final UserPlatformRepository userPlatformRepository;
    private final YoutubeMusicSyncService youtubeMusicSyncService;
    private final Clock clock;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void resync() {
        LocalDateTime before = LocalDateTime.now(clock).minusDays(RESYNC_AFTER_DAYS);
        List<Long> userIds = userPlatformRepository.findUserIdsToResync(Platform.YOUTUBE_MUSIC, before);
        int failed = 0;
        for (Long userId : userIds) {
            try {
                youtubeMusicSyncService.sync(userId);
            } catch (CustomException e) {
                failed++;
                log.warn("[YoutubeMusicResyncScheduler] 재동기화 실패: userId = {}, code = {}", userId, e.getErrorCode().getCode());
            } catch (RuntimeException e) {
                failed++;
                log.error("[YoutubeMusicResyncScheduler] 재동기화 오류: userId = {}", userId, e);
            }
        }
        log.info("[YoutubeMusicResyncScheduler] 재동기화 완료: 대상 {}명, 실패 {}명", userIds.size(), failed);
    }
}
