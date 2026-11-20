package com.tuneet.tuneetbe.domain.feed.dto.response;

import com.tuneet.tuneetbe.domain.feed.enums.SentMessageType;
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
@Schema(title = "보낸 코멘트 응답 dto", description = "사용자가 보낸 코멘트 목록을 조회할때 코멘트나 답장 하나의 정보")
public class SentFeedMessageResponse {

    @Schema(description = "보낸 종류 (COMMENT: 코멘트, REPLY: 답장)", example = "COMMENT")
    private SentMessageType type;

    @Schema(description = "피드 메시지 고유번호 (답장이면 답장이 이어지는 처음 코멘트)", example = "1")
    private Long feedMessageId;

    @Schema(description = "답장 고유번호 (코멘트면 null)", example = "12")
    private Long replyId;

    @Schema(description = "코멘트를 보낸 피드 고유번호", example = "3")
    private Long feedId;

    @Schema(description = "받은 사람 고유번호 (코멘트면 피드 작성자, 답장이면 답장 받은 사람)", example = "2")
    private Long receiverId;

    @Schema(description = "받은 사람 닉네임 (탈퇴했으면 -> 탈퇴한 사용자)", example = "튜니")
    private String receiverNickname;

    @Schema(description = "코멘트나 답장과 함께 보낸 노래")
    private MusicResponse music;

    @Schema(description = "코멘트나 답장 내용", example = "이 노래도 좋아하실 것 같아요!")
    private String messageContent;

    @Schema(description = "보낸 시간", example = "2026-11-13T10:40:00")
    private LocalDateTime createdAt;
}
