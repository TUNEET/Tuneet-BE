package com.tuneet.tuneetbe.domain.like.dto.response;

import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.user.enums.LikeSource;
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
@Schema(title = "좋아요한 곡 목록 응답 dto", description = "앱에서 누른 좋아요와 유튜브 뮤직에서 불러온 좋아요를 합친 목록 (같은 곡은 한 번, 최근 좋아요한 순)")
public class LikedMusicListResponse {

    @Schema(description = "좋아요한 곡 목록")
    private List<LikedMusic> musics;

    @Schema(description = "다음 페이지가 있는지", example = "true")
    private Boolean hasNext;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "좋아요한 곡")
    public static class LikedMusic {

        @Schema(description = "좋아요 고유번호 (다음 페이지 조회의 cursor)", example = "42")
        private Long likeId;

        @Schema(description = "곡 정보")
        private MusicResponse music;

        @Schema(description = "좋아요 출처 (IN_APP: 앱에서 누름, YOUTUBE_MUSIC: 유튜브 뮤직에서 불러옴)", example = "IN_APP")
        private LikeSource source;

        @Schema(description = "좋아요 일시 (불러온 좋아요는 불러온 일시)", example = "2026-11-16T15:00:00")
        private LocalDateTime likedAt;
    }
}
