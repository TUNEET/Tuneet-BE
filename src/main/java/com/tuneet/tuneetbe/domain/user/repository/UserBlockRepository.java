package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    // 사용자가 차단하거나 차단당한 기록 전체 삭제
    void deleteAllByBlockerIdOrBlockedId(Long blockerId, Long blockedId);
}
