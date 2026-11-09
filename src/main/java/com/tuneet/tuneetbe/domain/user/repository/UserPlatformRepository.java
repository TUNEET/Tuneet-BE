package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserPlatform;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPlatformRepository extends JpaRepository<UserPlatform, Long> {

    // 음원 플랫폼 연동 여부 조회
    boolean existsByUserId(Long userId);

    // 사용자의 음원 플랫폼 연동 정보 전체 삭제
    void deleteAllByUserId(Long userId);
}
