package com.tuneet.tuneetbe.domain.feed.dto.response;

import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
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
@Schema(title = "보낸 코멘트 응답 dto", description = "사용자가 보낸 코멘트 목록을 조회할때 코멘트 하나의 정보")
public class SentFeedMessageResponse {

    @Schema(description = "피드 메시지 고유번호", example = "1")
    private Long feedMessageId;

    @Schema(description = "코멘트를 보낸 피드 고유번호", example = "3")
    private Long feedId;

    @Schema(description = "받은 사람(피드 작성자) 고유번호", example = "2")
    private Long receiverId;

    @Schema(description = "받은 사람(피드 작성자) 닉네임", example = "튜니")
    private String receiverNickname;

    @Schema(description = "코멘트와 함께 보낸 노래")
    private MusicResponse music;

    @Schema(description = "코멘트 내용", example = "이 노래도 좋아하실 것 같아요!")
    private String messageContent;

    @Schema(description = "코멘트 보낸 시간", example = "2026-11-13T10:40:00")
    private LocalDateTime createdAt;
}
