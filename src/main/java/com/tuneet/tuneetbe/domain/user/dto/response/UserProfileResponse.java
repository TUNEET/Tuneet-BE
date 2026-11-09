package com.tuneet.tuneetbe.domain.user.dto.response;

import com.tuneet.tuneetbe.domain.music.dto.response.ArtistResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.GenreResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
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

    @Schema(description = "유사도 (%)", example = "92")
    private Integer similarity;

    @Schema(description = "거리 (km)", example = "1.2")
    private Double distanceKm;

    @Schema(description = "편지 받기 여부", example = "true")
    private Boolean letterReceivable;

    @Schema(description = "나와 함께 좋아하는 항목")
    private Common common;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "함께 좋아하는 항목 응답 dto", description = "나와 다른 사용자가 함께 좋아하는 아티스트, 노래, 장르")
    public static class Common {

        @Schema(description = "함께 좋아하는 아티스트 목록")
        private List<ArtistResponse> artists;

        @Schema(description = "함께 좋아하는 노래")
        private MusicResponse music;

        @Schema(description = "함께 좋아하는 장르 목록")
        private List<GenreResponse> genres;
    }
}
