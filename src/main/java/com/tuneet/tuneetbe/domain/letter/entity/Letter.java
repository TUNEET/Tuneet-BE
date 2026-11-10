package com.tuneet.tuneetbe.domain.letter.entity;

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
@Table(name = "letters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Letter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "letter_id")
    private Long id;

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
    private String letterContent; // 편지 내용 (최대 100자)

    @Column(nullable = false)
    private Boolean isRandom; // 랜덤 편지 여부

    @Column(nullable = false)
    private Boolean deletedByReceiver = false; // 수신자 삭제 여부

    private LocalDateTime readAt; // 읽은 일시

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 보낸 일시

    @Builder
    private Letter(User sender, User receiver, Music music, String letterContent, Boolean isRandom) {
        this.sender = sender;
        this.receiver = receiver;
        this.music = music;
        this.letterContent = letterContent;
        this.isRandom = isRandom;
    }

    // 처음 읽을 때만 읽은 시간 저장
    public void markAsRead(LocalDateTime readAt) {
        if (this.readAt == null) {
            this.readAt = readAt;
        }
    }

    // 받는 사람의 편지함에서 삭제 처리
    public void deleteByReceiver() {
        this.deletedByReceiver = true;
    }
}
