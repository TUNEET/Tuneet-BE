package com.tuneet.tuneetbe.domain.music.dto.response;

import com.tuneet.tuneetbe.domain.music.enums.GenreType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "장르 응답 dto", description = "장르 정보")
public class GenreResponse {

    @Schema(description = "장르 고유번호", example = "1")
    private Long genreId;

    @Schema(description = "장르", example = "K_POP")
    private GenreType genreName;

    @Schema(description = "화면 표시 이름", example = "K-POP")
    private String displayName;
}
