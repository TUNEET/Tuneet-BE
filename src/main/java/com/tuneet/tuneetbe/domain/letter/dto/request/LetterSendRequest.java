package com.tuneet.tuneetbe.domain.letter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "편지 발송 dto", description = "사용자가 편지를 보낼때 요청 보내는 데이터")
public class LetterSendRequest {

    @Schema(description = "노래 검색 결과의 id", example = "27e95178-49ac-47a1-8b4c-540c77982680")
    @NotBlank(message = "노래는 필수 입력값입니다.")
    @Pattern(regexp = "^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$", message = "올바른 노래 id가 아닙니다.")
    private String musicId;

    @Schema(description = "편지 내용 (최대 100자)", example = "이 노래 너무 좋아요!")
    @NotBlank(message = "편지 내용은 필수 입력값입니다.")
    @Size(max = 100, message = "편지 내용은 100자 이하여야 합니다.")
    private String letterContent;

    @Schema(description = "받는 사람 고유번호", example = "2")
    @NotNull(message = "받는 사람은 필수 입력값입니다.")
    private Long receiverId;
}
