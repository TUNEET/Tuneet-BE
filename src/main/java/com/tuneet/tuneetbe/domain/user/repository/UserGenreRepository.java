package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserGenre;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGenreRepository extends JpaRepository<UserGenre, Long> {

    // 사용자의 선호 장르를 장르와 함께 표시 순서대로 조회
    // @EntityGraph로 장르를 함께 조회해서, 목록을 돌며 장르를 읽을 때 추가 쿼리가 생기지 않도록 함
    @EntityGraph(attributePaths = "genre")
    List<UserGenre> findAllByUserIdOrderByGenreDisplayOrderAsc(Long userId);

    // 사용자의 선호 장르 전체 삭제
    // 호출 즉시 DELETE를 실행하지 않고 flush 시점에 실행
    void deleteAllByUserId(Long userId);
}
