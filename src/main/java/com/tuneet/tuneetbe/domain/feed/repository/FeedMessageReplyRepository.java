package com.tuneet.tuneetbe.domain.feed.repository;

import com.tuneet.tuneetbe.domain.feed.entity.FeedMessageReply;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedMessageReplyRepository extends JpaRepository<FeedMessageReply, Long> {

    // 코멘트에 답장이 하나라도 있는지 확인
    boolean existsByFeedMessageId(Long feedMessageId);

    // 같은 코멘트에 이 답장보다 나중 답장이 있는지 확인 (있으면 이미 답장한 것)
    boolean existsByFeedMessageIdAndIdGreaterThan(Long feedMessageId, Long replyId);

    // 답장을 원본 코멘트, 코멘트 보낸 사람, 코멘트 노래와 함께 조회
    @EntityGraph(attributePaths = {"feedMessage", "feedMessage.user", "feedMessage.music"})
    Optional<FeedMessageReply> findWithDetailById(Long replyId);

    // 코멘트에 주고받은 답장 전체를 보낸 사람, 노래와 함께 오래된 순으로 조회
    @EntityGraph(attributePaths = {"sender", "music"})
    List<FeedMessageReply> findAllByFeedMessageIdOrderByIdAsc(Long feedMessageId);

    // 코멘트 ID 목록에 달린 답장 전체를 보낸 사람, 노래와 함께 오래된 순으로 조회
    @EntityGraph(attributePaths = {"sender", "music"})
    List<FeedMessageReply> findAllByFeedMessageIdInOrderByIdAsc(List<Long> feedMessageIds);

    // 답장 ID 목록으로 노래, 코멘트와 함께 조회
    @EntityGraph(attributePaths = {"music", "feedMessage"})
    List<FeedMessageReply> findAllByIdIn(List<Long> ids);

    // 코멘트별 가장 최근 답장 ID 조회 ([코멘트 ID, 최근 답장 ID])
    @Query("select r.feedMessage.id, max(r.id) from FeedMessageReply r where r.feedMessage.id in :feedMessageIds group by r.feedMessage.id")
    List<Object[]> findLatestReplyIds(@Param("feedMessageIds") List<Long> feedMessageIds);

    // 코멘트 ID 목록에 달린 답장 ID 조회
    @Query("select r.id from FeedMessageReply r where r.feedMessage.id in :feedMessageIds")
    List<Long> findIdsByFeedMessageIdIn(@Param("feedMessageIds") List<Long> feedMessageIds);

    // 코멘트 ID 목록에 달린 답장을 한 번에 삭제 (한 건씩 조회 후 삭제하지 않음)
    @Modifying
    @Query("delete from FeedMessageReply r where r.feedMessage.id in :feedMessageIds")
    void deleteAllByFeedMessageIdIn(@Param("feedMessageIds") List<Long> feedMessageIds);
}
