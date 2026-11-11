package com.tuneet.tuneetbe.domain.notification.dto.response;

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
@Schema(title = "알림 목록 조회 응답 dto", description = "사용자가 알림 목록을 조회할때 서버에서 보내는 결과")
public class NotificationListResponse {

    @Schema(description = "알림 목록")
    private List<NotificationResponse> notifications;

    @Schema(description = "다음 알림 존재 여부", example = "true")
    private Boolean hasNext;
}
