package com.tuneet.tuneetbe.domain.matching.entity;

import com.tuneet.tuneetbe.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_recommendation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 추천받은 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_user_id", nullable = false)
    private User recommendedUser; // 추천된 사용자

    @Column(nullable = false)
    private Integer displayOrder; // 화면 표시 순서

    @Column(nullable = false)
    private LocalDateTime createdAt; // 추천 일시 (같이 추천된 사용자는 같은 값)

    @Builder
    private UserRecommendation(User user, User recommendedUser, Integer displayOrder, LocalDateTime createdAt) {
        this.user = user;
        this.recommendedUser = recommendedUser;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
    }
}
