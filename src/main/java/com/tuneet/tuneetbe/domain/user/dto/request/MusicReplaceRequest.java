package com.tuneet.tuneetbe.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "좋아하는 노래 교체 dto", description = "사용자가 좋아하는 노래를 교체할 때 요청 보내는 데이터")
public class MusicReplaceRequest {

    @Schema(description = "노래 검색 결과의 id 목록 (1~20개)", example = "[\"27e95178-49ac-47a1-8b4c-540c77982680\"]")
    @NotEmpty(message = "노래는 1개 이상 선택해야 합니다.")
    @Size(max = 20, message = "노래는 20개 이하로 선택해야 합니다.")
    private LinkedHashSet<
            @NotNull(message = "올바른 노래 id가 아닙니다.")
            @Pattern(regexp = "^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$", message = "올바른 노래 id가 아닙니다.")
            String> musicIds;
}
