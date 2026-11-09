package com.tuneet.tuneetbe.domain.feed.repository;

import com.tuneet.tuneetbe.domain.feed.entity.FeedReaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedReactionRepository extends JpaRepository<FeedReaction, Long> {

    // 사용자가 작성한 피드에 달린 반응 전체 삭제
    void deleteAllByFeedUserId(Long userId);
}
