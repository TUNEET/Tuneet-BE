package com.tuneet.tuneetbe.domain.notification.dto.response;

import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "알림 응답 dto", description = "알림 하나의 정보")
public class NotificationResponse {

    @Schema(description = "알림 고유번호", example = "1")
    private Long notificationId;

    @Schema(description = "알림 종류", example = "FEED_MESSAGE")
    private NotificationType type;

    @Schema(description = "보낸 사람 닉네임 (탈퇴한 경우 -> 탈퇴한 사용자)", example = "test01")
    private String senderNickname;

    @Schema(description = "메시지가 달린 피드 고유번호 (누르면 피드 상세로 이동)", example = "3")
    private Long feedId;

    @Schema(description = "메시지 내용", example = "이 노래도 좋아하실 것 같아요!")
    private String messageContent;

    @Schema(description = "메시지와 함께 보낸 노래")
    private MusicResponse music;

    @Schema(description = "읽음 여부", example = "false")
    private Boolean isRead;

    @Schema(description = "알림 생성 시간", example = "2026-11-11T10:40:00")
    private LocalDateTime createdAt;
}
