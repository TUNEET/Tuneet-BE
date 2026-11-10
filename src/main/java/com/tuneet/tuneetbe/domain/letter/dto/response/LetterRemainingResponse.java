package com.tuneet.tuneetbe.domain.letter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "남은 편지 수량 응답 dto", description = "사용자의 남은 편지 수량을 서버에서 보내는 결과")
public class LetterRemainingResponse {

    @Schema(description = "오늘 남은 편지지 수", example = "3")
    private Integer remaining;

    @Schema(description = "하루 최대 편지지 수", example = "5")
    private Integer limit;
}
