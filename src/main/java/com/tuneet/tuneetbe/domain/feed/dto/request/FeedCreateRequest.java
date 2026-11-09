package com.tuneet.tuneetbe.domain.feed.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "피드 작성 dto", description = "사용자가 피드를 작성할때 요청 보내는 데이터")
public class FeedCreateRequest {

    @Schema(description = "노래 검색 결과의 id", example = "27e95178-49ac-47a1-8b4c-540c77982680")
    @NotBlank(message = "노래는 필수 입력값입니다.")
    @Pattern(regexp = "^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$", message = "올바른 노래 id가 아닙니다.")
    private String musicId;

    @Schema(description = "코멘트 (최대 100자)", example = "이 노래 너무 좋아요!")
    @NotBlank(message = "코멘트는 필수 입력값입니다.")
    @Size(max = 100, message = "코멘트는 100자 이하여야 합니다.")
    private String content;
}
