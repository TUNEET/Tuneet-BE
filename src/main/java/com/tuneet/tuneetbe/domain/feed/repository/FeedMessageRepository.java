package com.tuneet.tuneetbe.domain.feed.repository;

import com.tuneet.tuneetbe.domain.feed.entity.FeedMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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

    // 내가 보낸 메시지를 최신순으로 피드, 피드 작성자, 노래와 함께 조회
    @EntityGraph(attributePaths = {"feed", "feed.user", "music"})
    Slice<FeedMessage> findByUserIdAndDeletedBySenderFalseOrderByIdDesc(Long userId, Pageable pageable);

    // 내가 보낸 메시지 중 커서보다 이전 메시지를 피드, 피드 작성자, 노래와 함께 조회
    @EntityGraph(attributePaths = {"feed", "feed.user", "music"})
    Slice<FeedMessage> findByUserIdAndDeletedBySenderFalseAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);

    // 내가 보내고 아직 보낸 목록에서 삭제하지 않은 메시지 전체 조회
    List<FeedMessage> findAllByUserIdAndDeletedBySenderFalse(Long userId);
}
