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
@Schema(title = "피드 코멘트 목록 응답 dto", description = "피드에 달린 코멘트 전체 (최신순, 코멘트마다 주고받은 답장 포함)")
public class FeedMessageListResponse {

    @Schema(description = "코멘트 목록 (최신순, 없으면 빈 목록)")
    private List<FeedMessageResponse> messages;
}
