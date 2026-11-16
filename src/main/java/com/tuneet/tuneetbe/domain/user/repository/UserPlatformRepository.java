package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserPlatform;
import com.tuneet.tuneetbe.domain.user.enums.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserPlatformRepository extends JpaRepository<UserPlatform, Long> {

    // 음원 플랫폼 연동 여부 조회
    boolean existsByUserId(Long userId);

    // 사용자의 음원 플랫폼 연동 정보 전체 삭제
    void deleteAllByUserId(Long userId);

    Optional<UserPlatform> findByUserIdAndPlatform(Long userId, Platform platform);

    List<UserPlatform> findAllByUserId(Long userId);

    // 마지막 동기화가 기준 일시보다 오래됐거나 한 번도 동기화하지 않은 연동의 사용자
    @Query("""
            SELECT p.user.id FROM UserPlatform p
            WHERE p.platform = :platform AND (p.lastSyncedAt IS NULL OR p.lastSyncedAt < :before)
            """)
    List<Long> findUserIdsToResync(@Param("platform") Platform platform, @Param("before") LocalDateTime before);
}
