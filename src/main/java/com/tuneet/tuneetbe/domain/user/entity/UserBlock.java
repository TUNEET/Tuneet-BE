package com.tuneet.tuneetbe.domain.user.entity;

import com.tuneet.tuneetbe.domain.user.enums.BlockType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_blocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_block_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker; // 차단한 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked; // 차단된 사용자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BlockType type; // 차단 / 숨김

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 차단 일시
}
