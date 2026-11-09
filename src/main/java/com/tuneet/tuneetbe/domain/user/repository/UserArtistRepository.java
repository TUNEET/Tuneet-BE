package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserArtist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserArtistRepository extends JpaRepository<UserArtist, Long> {

    // 사용자의 좋아하는 아티스트 전체 삭제
    void deleteAllByUserId(Long userId);
}
