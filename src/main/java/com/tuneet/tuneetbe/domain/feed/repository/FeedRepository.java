package com.tuneet.tuneetbe.domain.feed.repository;

import com.tuneet.tuneetbe.domain.feed.entity.Feed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    // 사용자가 작성한 피드 전체 삭제
    void deleteAllByUserId(Long userId);

    // 최신 피드부터 작성자와 노래를 함께 조회
    @EntityGraph(attributePaths = {"user", "music"})
    Slice<Feed> findAllByOrderByIdDesc(Pageable pageable);

    // 커서보다 이전 피드를 작성자와 노래를 함께 조회
    @EntityGraph(attributePaths = {"user", "music"})
    Slice<Feed> findByIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);
}
