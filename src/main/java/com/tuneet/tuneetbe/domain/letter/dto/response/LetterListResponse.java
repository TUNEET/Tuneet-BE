package com.tuneet.tuneetbe.domain.letter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "편지 리스트 조회 응답 dto", description = "사용자가 편지 리스트를 조회할때 서버에서 보내는 결과")
public class LetterListResponse {

    @Schema(description = "편지 목록")
    private List<LetterDetailResponse> letters;

    @Schema(description = "다음 편지 존재 여부", example = "true")
    private Boolean hasNext;
}
