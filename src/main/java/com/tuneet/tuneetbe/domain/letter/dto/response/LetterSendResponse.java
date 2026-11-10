package com.tuneet.tuneetbe.domain.letter.dto.response;

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
@Schema(title = "편지 응답 dto", description = "사용자가 다른 사용자에게 편지를 보낼때 서버에서 보내는 결과")
public class LetterSendResponse {

    @Schema(description = "편지 고유번호", example = "1")
    private Long letterId;

    @Schema(description = "보낸 사람 닉네임", example = "튜니")
    private String senderNickname;

    @Schema(description = "편지에서 보낸 노래")
    private MusicResponse music;

    @Schema(description = "편지 내용", example = "이 노래 너무 좋아요!")
    private String letterContent;

    @Schema(description = "받는 사람 닉네임", example = "원이")
    private String receiverNickname;

    @Schema(description = "편지 생성 시간", example = "2026-11-07T10:40:00")
    private LocalDateTime createdAt;
}
