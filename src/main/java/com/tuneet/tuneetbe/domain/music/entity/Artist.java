package com.tuneet.tuneetbe.domain.music.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "artists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "artist_id")
    private Long id;

    @Column(nullable = false)
    private String name; // 아티스트 이름

    private String artistImage; // 아티스트 이미지

    @Column(nullable = false, unique = true)
    private String externalId; // 외부 음원 서비스 ID
}
