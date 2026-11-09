package com.tuneet.tuneetbe.domain.music.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "노래 검색 결과")
public record MusicSearchItemResponse(
        @Schema(description = "녹음 MBID", example = "27e95178-49ac-47a1-8b4c-540c77982680")
        String id,

        @Schema(description = "같은 곡의 음원이 공유하는 대표 ID")
        String canonicalId,

        @Schema(description = "노래 제목", example = "FAKE LOVE")
        String title,

        @Schema(description = "곡에 표기된 아티스트", example = "BTS")
        String artist,

        @Schema(description = "아티스트 목록. 첫 번째가 대표 아티스트이며 빈 배열일 수 있음")
        List<MusicArtist> artists,

        @Schema(description = "앨범명", nullable = true)
        String album,

        @Schema(description = "곡 길이(ms)", nullable = true)
        Integer durationMs,

        @Schema(description = "발매 연도", nullable = true)
        Integer releaseYear,

        @Schema(description = "앨범 커버 URL", nullable = true)
        String coverUrl) {

    @Schema(description = "곡의 아티스트")
    public record MusicArtist(
            @Schema(description = "아티스트 MBID")
            String id,

            @Schema(description = "아티스트 이름", example = "BTS")
            String name) {
    }
}
