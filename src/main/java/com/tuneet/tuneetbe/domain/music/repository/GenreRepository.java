package com.tuneet.tuneetbe.domain.music.repository;

import com.tuneet.tuneetbe.domain.music.entity.Genre;
import com.tuneet.tuneetbe.domain.music.enums.GenreType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    // 장르 이름 목록으로 표시 순서대로 조회
    List<Genre> findAllByGenreNameInOrderByDisplayOrderAsc(Collection<GenreType> genreNames);
}
