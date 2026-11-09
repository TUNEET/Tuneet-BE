package com.tuneet.tuneetbe.domain.music.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "검색 결과 페이지")
public record SearchPageResponse<T>(
        @Schema(description = "검색 결과 목록")
        List<T> items,

        @Schema(description = "현재 페이지", example = "1")
        int page,

        @Schema(description = "페이지 크기", example = "5")
        int size,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "전체 페이지 수. 결과가 많으면 최대 20", example = "3")
        int totalPages) {
}
