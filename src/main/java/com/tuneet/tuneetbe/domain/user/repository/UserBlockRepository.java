package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserBlock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    // 사용자가 차단하거나 차단당한 기록 전체 삭제
    void deleteAllByBlockerIdOrBlockedId(Long blockerId, Long blockedId);

    // 이미 차단했는지 확인
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    // 내가 특정 사용자를 차단한 기록 조회
    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    // 내가 차단한 사용자 목록을 최근 차단순으로 차단된 사용자와 함께 조회
    @EntityGraph(attributePaths = "blocked")
    List<UserBlock> findAllByBlockerIdOrderByIdDesc(Long blockerId);

    // 두 사용자 사이에 차단 기록이 있는지 확인 (A가 B를 차단했거나, B가 A를 차단한 경우 모두)
    // (blocker = A and blocked = B) or (blocker = B and blocked = A) 로 조회 (And가 Or보다 먼저 묶임)
    boolean existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(Long blockerId1, Long blockedId1, Long blockerId2, Long blockedId2);

    // 내가 차단했거나 나를 차단한 기록 전체 조회 (피드 목록, 랜덤 편지 후보에서 제외할 사용자 찾기)
    List<UserBlock> findAllByBlockerIdOrBlockedId(Long blockerId, Long blockedId);
}
