package com.tuneet.tuneetbe.domain.music.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "음악 응답 dto", description = "음악 정보")
public class MusicResponse {

    @Schema(description = "음악 고유번호", example = "1")
    private Long musicId;

    @Schema(description = "노래 제목", example = "노래 제목")
    private String title;

    @Schema(description = "아티스트 이름", example = "아티스트")
    private String artist;

    @Schema(description = "앨범 커버 이미지 url", example = "https://...")
    private String musicPicture;
}
