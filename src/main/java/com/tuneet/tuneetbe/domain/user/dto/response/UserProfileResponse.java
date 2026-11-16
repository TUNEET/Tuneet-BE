package com.tuneet.tuneetbe.domain.user.dto.response;

import com.tuneet.tuneetbe.domain.music.dto.response.ArtistResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.GenreResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.user.enums.DistanceRange;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "다른 사용자 프로필 조회 응답 dto", description = "다른 사용자의 프로필을 조회할때 서버에서 보내는 결과")
public class UserProfileResponse {

    @Schema(description = "사용자 고유번호", example = "2")
    private Long userId;

    @Schema(description = "닉네임", example = "test01")
    private String nickname;

    @Schema(description = "프로필 사진 url", example = "https://...")
    private String profilePicture;

    @Schema(description = "프로필 음악")
    private MusicResponse profileMusic;

    @Schema(description = "선호 장르 목록")
    private List<GenreResponse> genres;

    @Schema(description = "좋아하는 아티스트 목록")
    private List<ArtistResponse> favoriteArtists;

    @Schema(description = "나와의 매칭률 (0~100, 0은 겹치는 취향 없음). 내가 위치 미동의·위치 노출 OFF이거나 한쪽의 취향 데이터가 없으면 null",
            example = "92", nullable = true)
    private Integer matchPercent;

    @Schema(description = "거리 구간. 나 또는 상대가 위치 미동의·위치 노출 OFF이거나 위치가 없거나 차단 관계면 null", example = "WITHIN_3KM", nullable = true)
    private DistanceRange distanceRange;

    @Schema(description = "편지 받기 여부", example = "true")
    private Boolean letterReceivable;

    @Schema(description = "내가 이 사용자를 차단했는지 여부", example = "false")
    private Boolean isBlocked;

    @Schema(description = "나와 함께 좋아하는 항목")
    private Common common;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "함께 좋아하는 항목 응답 dto", description = "나와 다른 사용자가 함께 좋아하는 아티스트, 노래, 장르")
    public static class Common {

        @Schema(description = "함께 좋아하는 아티스트 목록 (선호 아티스트와 좋아요한 아티스트를 합쳐 비교)")
        private List<ArtistResponse> artists;

        @Schema(description = "함께 좋아하는 노래 (프로필 음악이 같으면 그 곡, 아니면 좋아하는 노래·좋아요한 노래 중 같은 곡 하나)", nullable = true)
        private MusicResponse music;

        @Schema(description = "함께 좋아하는 장르 목록")
        private List<GenreResponse> genres;
    }
}
