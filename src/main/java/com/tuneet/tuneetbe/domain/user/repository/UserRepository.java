package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // 이메일이 존재하는지 확인
    boolean existsByEmail(String email);

    // 닉네임이 존재하는지 확인
    boolean existsByNickname(String nickname);

    // 행 잠금을 걸고 사용자 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    // 랜덤 편지를 받을 수 있는 사용자 수 (나 제외, 탈퇴하지 않음, 편지 받기 켬)
    // 나와 차단 관계인 사용자도 제외
    long countByIdNotInAndDeletedAtIsNullAndLetterReceivableTrue(List<Long> excludedUserIds);

    // 랜덤 편지를 받을 수 있는 사용자 조회 (나 제외, 탈퇴하지 않음, 편지 받기 켬)
    // 나와 차단 관계인 사용자도 제외
    List<User> findByIdNotInAndDeletedAtIsNullAndLetterReceivableTrueOrderByIdAsc(List<Long> excludedUserIds, Pageable pageable);

    // 추천 후보를 가까운 순으로 조회
    // 프로필 작성, 위치 동의·노출, since 이후 위치 갱신, 차단 관계(양방향) 아님
    @Query(value = """
            SELECT u.* FROM users u
            WHERE u.user_id <> :userId
              AND u.deleted_at IS NULL
              AND u.nickname IS NOT NULL
              AND u.location_agreed_at IS NOT NULL
              AND u.location_visible = TRUE
              AND u.latitude IS NOT NULL AND u.longitude IS NOT NULL
              AND u.location_updated_at >= :since
              AND NOT EXISTS (
                  SELECT 1 FROM user_blocks b
                  WHERE (b.blocker_id = :userId AND b.blocked_id = u.user_id)
                     OR (b.blocker_id = u.user_id AND b.blocked_id = :userId))
            ORDER BY ST_Distance_Sphere(POINT(u.longitude, u.latitude), POINT(:longitude, :latitude)), u.user_id
            LIMIT :limit
            """, nativeQuery = true)
    List<User> findRecommendationCandidates(
            @Param("userId") Long userId,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("since") LocalDateTime since,
            @Param("limit") int limit);
}
