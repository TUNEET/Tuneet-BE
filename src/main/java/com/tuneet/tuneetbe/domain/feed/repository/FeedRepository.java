package com.tuneet.tuneetbe.domain.feed.repository;

import com.tuneet.tuneetbe.domain.feed.entity.Feed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    // 사용자가 작성한 피드 전체 삭제
    void deleteAllByUserId(Long userId);

    // 최신 피드부터 작성자와 노래를 함께 조회
    @EntityGraph(attributePaths = {"user", "music"})
    Slice<Feed> findAllByOrderByIdDesc(Pageable pageable);

    // 커서보다 이전 피드를 작성자와 노래를 함께 조회
    @EntityGraph(attributePaths = {"user", "music"})
    Slice<Feed> findByIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);

    // 최신 피드부터 작성자와 노래를 함께 조회 (차단 관계인 사용자의 피드 제외)
    // 제외할 사용자 목록이 비어 있으면 NOT IN 조회가 의도대로 동작하지 않을 수 있어 차단 관계가 있을 때만 사용
    @EntityGraph(attributePaths = {"user", "music"})
    Slice<Feed> findByUserIdNotInOrderByIdDesc(List<Long> excludedUserIds, Pageable pageable);

    // 커서보다 이전 피드를 작성자와 노래를 함께 조회 (차단 관계인 사용자의 피드 제외)
    @EntityGraph(attributePaths = {"user", "music"})
    Slice<Feed> findByUserIdNotInAndIdLessThanOrderByIdDesc(List<Long> excludedUserIds, Long cursor, Pageable pageable);
}
