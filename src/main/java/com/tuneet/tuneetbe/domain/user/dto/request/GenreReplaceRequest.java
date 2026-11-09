package com.tuneet.tuneetbe.domain.user.dto.request;

import com.tuneet.tuneetbe.domain.music.enums.GenreType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "선호 장르 교체 dto", description = "사용자가 선호 장르를 교체할 때 요청 보내는 데이터")
public class GenreReplaceRequest {

    @Schema(description = "선호 장르 목록 (1개 이상)", example = "[\"K_POP\", \"POP\"]")
    @NotEmpty(message = "장르는 1개 이상 선택해야 합니다.")
    private Set<@NotNull(message = "장르는 null일 수 없습니다.") GenreType> genres;
}
