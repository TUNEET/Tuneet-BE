package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserArtist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserArtistRepository extends JpaRepository<UserArtist, Long> {

    List<UserArtist> findAllByUserId(Long userId);

    // 사용자의 좋아하는 아티스트를 아티스트 정보와 함께 조회 (저장한 순서)
    @EntityGraph(attributePaths = "artist")
    List<UserArtist> findAllWithArtistByUserIdOrderByIdAsc(Long userId);

    // 사용자의 좋아하는 아티스트 전체 삭제
    void deleteAllByUserId(Long userId);
}
