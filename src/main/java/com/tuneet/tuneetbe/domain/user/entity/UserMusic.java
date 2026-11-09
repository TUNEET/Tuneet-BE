package com.tuneet.tuneetbe.domain.user.entity;

import com.tuneet.tuneetbe.domain.music.entity.Music;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_musics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMusic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_music_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id", nullable = false)
    private Music music; // 좋아하는 노래 (1곡 이상)

    @Builder
    private UserMusic(User user, Music music) {
        this.user = user;
        this.music = music;
    }
}
