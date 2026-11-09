package com.tuneet.tuneetbe.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(title = "선호 아티스트 교체 dto", description = "사용자가 선호 아티스트를 교체할 때 요청 보내는 데이터")
public class ArtistReplaceRequest {

    @Schema(description = "아티스트 검색 결과의 id 목록 (3~20팀)", example = "[\"b9545342-1e6d-4dae-84ac-013374ad8d7c\"]")
    @NotNull(message = "아티스트는 3팀 이상 선택해야 합니다.")
    @Size(min = 3, max = 20, message = "아티스트는 3팀 이상 20팀 이하로 선택해야 합니다.")
    private LinkedHashSet<
            @Pattern(regexp = "^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$", message = "올바른 아티스트 id가 아닙니다.")
            String> artistIds;
}
