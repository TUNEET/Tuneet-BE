package com.tuneet.tuneetbe.domain.room.entity;

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
@Table(name = "room_playlists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RoomPlaylist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_playlist_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id", nullable = false)
    private Music music; // 플레이리스트 노래

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 노래를 추가한 사용자

    @Column(nullable = false)
    private Integer position; // 재생 순서

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 추가 일시

    @Builder
    public RoomPlaylist(Room room, Music music, User user, Integer position) {
        this.room = room;
        this.music = music;
        this.user = user;
        this.position = position;
    }

    public void changePosition(int i) {
        this.position = i;
    }
}
