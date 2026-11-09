package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserGenreRepository extends JpaRepository<UserGenre, Long> {

    // 사용자의 선호 장르를 장르와 함께 표시 순서대로 조회
    @Query("select ug from UserGenre ug join fetch ug.genre g where ug.user.id = :userId order by g.displayOrder asc")
    List<UserGenre> findAllWithGenreByUserId(@Param("userId") Long userId);

    // 사용자의 선호 장르 전체 삭제
    @Modifying
    @Query("delete from UserGenre ug where ug.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
