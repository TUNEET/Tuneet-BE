package com.tuneet.tuneetbe.domain.feed.dto.response;

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
@Schema(title = "보낸 코멘트 리스트 조회 응답 dto", description = "사용자가 보낸 코멘트 리스트를 조회할때 서버에서 보내는 결과")
public class SentFeedMessageListResponse {

    @Schema(description = "보낸 코멘트 목록")
    private List<SentFeedMessageResponse> messages;

    @Schema(description = "다음 코멘트 존재 여부", example = "true")
    private Boolean hasNext;
}
