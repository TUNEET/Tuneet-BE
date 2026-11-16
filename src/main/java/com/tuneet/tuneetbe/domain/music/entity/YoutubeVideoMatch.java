package com.tuneet.tuneetbe.domain.music.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "youtube_video_matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class YoutubeVideoMatch {

    // 이 기간이 지난 매칭은 다시 매칭
    public static final Duration MAX_AGE = Duration.ofDays(30);

    @Id
    @Column(length = 32)
    private String videoId; // 유튜브 영상 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id", nullable = false)
    private Music music; // 매칭된 곡 (커버 영상이면 원곡)

    @Column(nullable = false)
    private Boolean isCover; // 커버 영상 여부

    @Column(nullable = false)
    private LocalDateTime matchedAt; // 매칭 일시

    @Builder
    private YoutubeVideoMatch(String videoId, Music music, Boolean isCover, LocalDateTime matchedAt) {
        this.videoId = videoId;
        this.music = music;
        this.isCover = isCover;
        this.matchedAt = matchedAt;
    }

    public boolean isFresh(LocalDateTime now) {
        return matchedAt.isAfter(now.minus(MAX_AGE));
    }
}
