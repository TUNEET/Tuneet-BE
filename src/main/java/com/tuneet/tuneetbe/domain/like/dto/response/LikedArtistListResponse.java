package com.tuneet.tuneetbe.domain.like.dto.response;

import com.tuneet.tuneetbe.domain.music.dto.response.ArtistResponse;
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
@Schema(title = "좋아요한 아티스트 목록 응답 dto", description = "좋아요한 아티스트 목록 (최근 좋아요한 순)")
public class LikedArtistListResponse {

    @Schema(description = "좋아요한 아티스트 목록")
    private List<LikedArtist> artists;

    @Schema(description = "다음 페이지가 있는지", example = "true")
    private Boolean hasNext;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "좋아요한 아티스트")
    public static class LikedArtist {

        @Schema(description = "좋아요 고유번호 (다음 페이지 조회의 cursor)", example = "7")
        private Long likeId;

        @Schema(description = "아티스트 정보")
        private ArtistResponse artist;

        @Schema(description = "좋아요 일시", example = "2026-11-16T15:00:00")
        private LocalDateTime likedAt;
    }
}
