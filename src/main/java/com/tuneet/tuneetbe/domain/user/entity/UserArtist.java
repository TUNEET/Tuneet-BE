package com.tuneet.tuneetbe.domain.user.entity;

import com.tuneet.tuneetbe.domain.music.entity.Artist;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_artists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserArtist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_artist_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist; // 선호 아티스트 (3팀 이상)

    @Builder
    private UserArtist(User user, Artist artist) {
        this.user = user;
        this.artist = artist;
    }
}
