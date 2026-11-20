package com.tuneet.tuneetbe.domain.feed.entity;

import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "feed_message_replies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FeedMessageReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_message_reply_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_message_id", nullable = false)
    private FeedMessage feedMessage; // 답장이 이어지는 처음 코멘트

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender; // 보낸 사람

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver; // 받는 사람

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id", nullable = false)
    private Music music; // 함께 보낼 노래

    @Column(nullable = false)
    private String replyContent; // 답장 내용 (최대 100자)

    @Column(nullable = false)
    private Boolean deletedBySender = false; // 발신자 삭제 여부 (보낸 목록에서만 숨김)

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 보낸 일시

    @Builder
    private FeedMessageReply(FeedMessage feedMessage, User sender, User receiver, Music music, String replyContent) {
        this.feedMessage = feedMessage;
        this.sender = sender;
        this.receiver = receiver;
        this.music = music;
        this.replyContent = replyContent;
    }

    // 보낸 사람의 보낸 목록에서 삭제 처리 (받은 사람의 알림, 답장 상세에는 그대로 남음)
    public void deleteBySender() {
        this.deletedBySender = true;
    }
}
