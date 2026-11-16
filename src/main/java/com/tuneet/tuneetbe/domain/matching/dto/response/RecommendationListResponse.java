package com.tuneet.tuneetbe.domain.matching.dto.response;

import com.tuneet.tuneetbe.domain.user.enums.DistanceRange;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "추천 사용자 목록 응답 dto", description = "마지막으로 받은 추천 사용자 (최대 5명, 매칭률 높은 순). 홈, 매칭 탭은 앞의 2명을 사용")
public class RecommendationListResponse {

    @Schema(description = "추천 일시 (추천을 받은 적이 없으면 null)", example = "2026-11-16T15:00:00", nullable = true)
    private LocalDateTime recommendedAt;

    @Schema(description = "추천 사용자 목록 (조건에 맞는 사용자가 없으면 빈 목록)")
    private List<RecommendedUser> users;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "추천 사용자 카드")
    public static class RecommendedUser {

        @Schema(description = "사용자 고유번호", example = "2")
        private Long userId;

        @Schema(description = "닉네임", example = "산책하는 튜니")
        private String nickname;

        @Schema(description = "프로필 사진 url", example = "https://...", nullable = true)
        private String profilePicture;

        @Schema(description = "나와의 매칭률 (0~100, 0은 겹치는 취향 없음). 상대의 취향 데이터가 없으면 null", example = "60", nullable = true)
        private Integer matchPercent;

        @Schema(description = "거리 구간", example = "WITHIN_3KM")
        private DistanceRange distanceRange;

        @Schema(description = "카드 문구에 쓸 함께 좋아하는 항목 (곡 → 아티스트 → 장르 순으로 하나, 없으면 null)", nullable = true)
        private Reason reason;

        @Schema(description = "편지 받기 여부", example = "true")
        private Boolean letterReceivable;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "추천 이유", description = "[name]를 좋아해요 / 나와 같은 노래(아티스트) 취향")
    public static class Reason {

        @Schema(description = "종류 (MUSIC: 노래, ARTIST: 아티스트, GENRE: 장르)", example = "MUSIC")
        private ReasonType type;

        @Schema(description = "노래 제목, 아티스트 이름 또는 장르 표시 이름", example = "Hype Boy")
        private String name;

        @Schema(description = "앨범 커버 또는 아티스트 이미지 (장르이거나 이미지가 없으면 null)", nullable = true)
        private String imageUrl;
    }

    public enum ReasonType {

        @Schema(description = "노래")
        MUSIC,

        @Schema(description = "아티스트")
        ARTIST,

        @Schema(description = "장르")
        GENRE
    }
}
