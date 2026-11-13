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
@Schema(title = "보낸 편지 응답 dto", description = "사용자가 보낸 편지 목록을 조회할때 편지 하나의 정보")
public class SentLetterResponse {

    @Schema(description = "편지 고유번호", example = "1")
    private Long letterId;

    @Schema(description = "받는 사람 고유번호", example = "2")
    private Long receiverId;

    @Schema(description = "받는 사람 닉네임 (탈퇴한 경우 -> 탈퇴한 사용자)", example = "튜니")
    private String receiverNickname;

    @Schema(description = "편지에서 보낸 노래")
    private MusicResponse music;

    @Schema(description = "편지 내용", example = "이 노래 너무 좋아요!")
    private String letterContent;

    @Schema(description = "랜덤 편지 여부", example = "false")
    private Boolean isRandom;

    @Schema(description = "답장 여부", example = "false")
    private Boolean isReply;

    @Schema(description = "편지 보낸 시간", example = "2026-11-13T10:40:00")
    private LocalDateTime createdAt;
}
