package com.tuneet.tuneetbe.domain.matching.repository;

import com.tuneet.tuneetbe.domain.matching.entity.UserRecommendation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRecommendationRepository extends JpaRepository<UserRecommendation, Long> {

    // 마지막으로 받은 추천을 추천된 사용자와 함께 표시 순서대로 조회
    @EntityGraph(attributePaths = "recommendedUser")
    List<UserRecommendation> findAllByUserIdOrderByDisplayOrderAsc(Long userId);

    // 사용자가 받은 추천과 다른 사용자에게 추천된 기록 전체 삭제
    void deleteAllByUserIdOrRecommendedUserId(Long userId, Long recommendedUserId);
}
