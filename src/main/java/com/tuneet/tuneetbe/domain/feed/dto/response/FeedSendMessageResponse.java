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
@Schema(title = "피드 메시지 응답 dto", description = "사용자가 다른 사용자에게 피드 메시지를 보낼때 서버에서 보내는 결과")
public class FeedSendMessageResponse {

    @Schema(description = "피드 메시지 고유번호", example = "1")
    private Long feedMessageId;

    @Schema(description = "피드 고유번호", example = "1")
    private Long feedId;

    @Schema(description = "사용자 닉네임", example = "test01")
    private String nickname;

    @Schema(description = "사용자 프로필 사진", example = "https://..")
    private String profilePicture;

    @Schema(description = "피드에서 코멘트한 노래")
    private MusicResponse music;

    @Schema(description = "피드 메시지 내용", example = "이 노래 너무 좋아요!")
    private String messageContent;

    @Schema(description = "피드 코멘트 생성 시간", example = "2026-11-07T10:40:00")
    private LocalDateTime createdAt;
}
