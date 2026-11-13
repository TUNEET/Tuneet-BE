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
@Table(name = "feed_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FeedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 코멘트 작성자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed; // 대상 피드

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id")
    private Music music; // 함께 보낼 노래

    @Column(nullable = false)
    private String messageContent; // 코멘트 내용 (최대 100자)

    @Column(nullable = false)
    private Boolean deletedBySender = false; // 발신자 삭제 여부 (보낸 목록에서만 숨김)

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 작성 일시

    @Builder
    private FeedMessage(User user, Feed feed, Music music, String messageContent) {
        this.user = user;
        this.feed = feed;
        this.music = music;
        this.messageContent = messageContent;
    }

    // 보낸 사람의 보낸 목록에서 삭제 처리 (받은 사람의 알림에는 그대로 남음)
    public void deleteBySender() {
        this.deletedBySender = true;
    }
}
