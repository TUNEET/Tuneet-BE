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
@Schema(title = "아티스트 응답 dto", description = "아티스트 정보")
public class ArtistResponse {

    @Schema(description = "아티스트 고유번호", example = "1")
    private Long artistId;

    @Schema(description = "아티스트 이름", example = "아티스트")
    private String name;

    @Schema(description = "아티스트 이미지 url", example = "https://...")
    private String artistImage;
}
