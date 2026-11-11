package com.tuneet.tuneetbe.domain.feed.repository;

import com.tuneet.tuneetbe.domain.feed.entity.FeedMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedMessageRepository extends JpaRepository<FeedMessage, Long> {

    // 사용자가 작성한 피드에 달린 메시지 전체 삭제
    void deleteAllByFeedUserId(Long userId);

    // 피드에 달린 메시지 전체 삭제
    void deleteAllByFeedId(Long feedId);

    // 피드에 달린 메시지 조회
    List<FeedMessage> findAllByFeedId(Long feedId);

    // 메시지 ID 목록으로 노래와 함께 조회
    @EntityGraph(attributePaths = "music")
    List<FeedMessage> findAllByIdIn(List<Long> ids);
}
