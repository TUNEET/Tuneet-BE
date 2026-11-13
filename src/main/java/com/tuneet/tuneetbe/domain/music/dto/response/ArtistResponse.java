package com.tuneet.tuneetbe.domain.music.dto.response;

import com.tuneet.tuneetbe.domain.music.entity.Artist;
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

    @Schema(description = "아티스트 검색 결과의 id", example = "b9545342-1e6d-4dae-84ac-013374ad8d7c")
    private String artistId;

    @Schema(description = "아티스트 이름", example = "아티스트")
    private String name;

    @Schema(description = "아티스트 이미지 url", example = "https://...")
    private String artistImage;

    public static ArtistResponse from(Artist artist) {
        return ArtistResponse.builder()
                .artistId(artist.getExternalId())
                .name(artist.getName())
                .artistImage(artist.getArtistImage())
                .build();
    }
}
