package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserMusic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMusicRepository extends JpaRepository<UserMusic, Long> {

    // 사용자의 좋아하는 노래 전체 삭제
    void deleteAllByUserId(Long userId);
}
