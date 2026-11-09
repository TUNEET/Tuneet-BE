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
@Schema(title = "피드 리스트 조회 응답 dto", description = "사용자가 피드 리스트를 조회할때 서버에서 보내는 결과")
public class FeedListResponse {

    @Schema(description = "피드 목록")
    private List<FeedDetailResponse> feeds;

    @Schema(description = "다음 피드 존재 여부", example = "true")
    private Boolean hasNext;
}
