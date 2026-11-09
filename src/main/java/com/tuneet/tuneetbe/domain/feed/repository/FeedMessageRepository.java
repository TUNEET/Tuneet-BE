package com.tuneet.tuneetbe.domain.feed.repository;

import com.tuneet.tuneetbe.domain.feed.entity.FeedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedMessageRepository extends JpaRepository<FeedMessage, Long> {

    // 사용자가 작성한 피드에 달린 메시지 전체 삭제
    void deleteAllByFeedUserId(Long userId);
}
