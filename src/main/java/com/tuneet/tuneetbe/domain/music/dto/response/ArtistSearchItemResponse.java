package com.tuneet.tuneetbe.domain.music.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아티스트 검색 결과")
public record ArtistSearchItemResponse(
        @Schema(description = "아티스트 MBID")
        String id,

        @Schema(description = "아티스트 이름", example = "IU")
        String name,

        @Schema(description = "같은 이름의 아티스트를 구분하는 설명", nullable = true)
        String disambiguation,

        @Schema(description = "수록곡 수", example = "320")
        int songCount) {
}
