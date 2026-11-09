package com.tuneet.tuneetbe.domain.music.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "music")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Music {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "music_id")
    private Long id;

    @Column(nullable = false)
    private String title; // 노래 제목

    @Column(nullable = false)
    private String artist; // 아티스트 이름

    private String musicPicture; // 앨범 커버 이미지

    @Column(nullable = false, unique = true)
    private String externalId; // 외부 음원 서비스 ID

    private String externalUrl; // 외부 음원 서비스 링크

    @Column(nullable = false, length = 36)
    private String canonicalId; // 같은 곡의 음원이 공유하는 대표 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist primaryArtist; // 대표 아티스트
}
