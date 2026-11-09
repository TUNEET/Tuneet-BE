package com.tuneet.tuneetbe.domain.music.entity;

import com.tuneet.tuneetbe.domain.music.enums.GenreType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "genres")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "genre_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private GenreType genreName; // 장르

    private Integer displayOrder; // 화면 표시 순서
}
