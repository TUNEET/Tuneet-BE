package com.tuneet.tuneetbe.domain.platform.dto.response;

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
@Schema(title = "유튜브 뮤직 좋아요 불러오기 응답 dto", description = "불러온 좋아요 곡 수와 선호 아티스트 후보")
public class PlatformSyncResponse {

    @Schema(description = "저장된 좋아요 곡 수 (유튜브 좋아요 중 곡을 찾은 것)", example = "142")
    private Integer likedMusicCount;

    @Schema(description = "곡을 찾지 못해 건너뛴 영상 수 (다음 불러오기 때 다시 시도)", example = "8")
    private Integer skippedVideoCount;

    @Schema(description = "좋아요에서 가장 많이 나온 아티스트 최대 3명 (저장하지 않음, 선호 아티스트 선택 화면의 후보)")
    private List<ArtistCandidate> artistCandidates;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "선호 아티스트 후보")
    public static class ArtistCandidate {

        @Schema(description = "아티스트 id (MBID, 선호 아티스트 교체 요청에 그대로 사용)", example = "0d79fe8e-ba27-4859-bb8c-2f255f346853")
        private String artistId;

        @Schema(description = "아티스트 이름", example = "BTS")
        private String name;

        @Schema(description = "이 아티스트의 좋아요 곡 수", example = "12")
        private Integer likedSongs;
    }
}
