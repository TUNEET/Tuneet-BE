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
@Schema(title = "코멘트 답장 응답 dto", description = "사용자가 코멘트나 답장에 답장을 보낼때 서버에서 보내는 결과")
public class FeedMessageReplySendResponse {

    @Schema(description = "답장 고유번호", example = "1")
    private Long replyId;

    @Schema(description = "답장이 이어지는 처음 코멘트 고유번호", example = "7")
    private Long feedMessageId;

    @Schema(description = "받는 사람 닉네임", example = "튜니")
    private String receiverNickname;

    @Schema(description = "답장과 함께 보낸 노래")
    private MusicResponse music;

    @Schema(description = "답장 내용", example = "추천 고마워요! 이 노래도 들어보세요")
    private String replyContent;

    @Schema(description = "보낸 시간", example = "2026-11-19T18:02:00")
    private LocalDateTime createdAt;
}
