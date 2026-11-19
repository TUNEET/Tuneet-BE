package com.tuneet.tuneetbe.domain.feed.dto.response;

import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "코멘트 답장 상세 응답 dto", description = "받은 답장 하나를 상세조회할때 서버에서 보내는 결과 (원 피드, 원본 코멘트, 주고받은 답장 전체 포함)")
public class FeedMessageReplyDetailResponse {

    @Schema(description = "조회한 답장 고유번호", example = "12")
    private Long replyId;

    @Schema(description = "원 게시물(피드) 고유번호", example = "3")
    private Long feedId;

    @Schema(description = "내가 조회한 답장에 이미 답장했는지 여부 (true면 답장 버튼 숨김)", example = "false")
    private Boolean isReplied;

    @Schema(description = "원본 코멘트")
    private OriginalMessage originalMessage;

    @Schema(description = "원본 코멘트에 주고받은 답장 전체 (오래된 순)")
    private List<Reply> replies;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "원본 코멘트 응답 dto", description = "답장이 시작된 처음 코멘트")
    public static class OriginalMessage {

        @Schema(description = "코멘트 고유번호", example = "7")
        private Long feedMessageId;

        @Schema(description = "코멘트 보낸 사람 고유번호", example = "2")
        private Long senderId;

        @Schema(description = "코멘트 보낸 사람 닉네임 (탈퇴했으면 -> 탈퇴한 사용자)", example = "노래하는 튜니")
        private String senderNickname;

        @Schema(description = "코멘트 보낸 사람 프로필 사진 (없거나 탈퇴했으면 null)", example = "https://example.com/profile.png")
        private String senderProfilePicture;

        @Schema(description = "내가 보낸 코멘트인지 여부", example = "true")
        private Boolean isMine;

        @Schema(description = "코멘트 내용", example = "이 노래도 좋아하실 것 같아요!")
        private String messageContent;

        @Schema(description = "코멘트와 함께 보낸 노래 (없으면 null)")
        private MusicResponse music;

        @Schema(description = "보낸 시간", example = "2026-11-19T17:40:00")
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "주고받은 답장 응답 dto", description = "원본 코멘트에 주고받은 답장 하나")
    public static class Reply {

        @Schema(description = "답장 고유번호", example = "12")
        private Long replyId;

        @Schema(description = "보낸 사람 고유번호", example = "1")
        private Long senderId;

        @Schema(description = "보낸 사람 닉네임 (탈퇴했으면 -> 탈퇴한 사용자)", example = "튜니")
        private String senderNickname;

        @Schema(description = "보낸 사람 프로필 사진 (없거나 탈퇴했으면 null)", example = "https://example.com/profile.png")
        private String senderProfilePicture;

        @Schema(description = "내가 보낸 답장인지 여부", example = "false")
        private Boolean isMine;

        @Schema(description = "답장과 함께 보낸 노래")
        private MusicResponse music;

        @Schema(description = "답장 내용", example = "요즘 출퇴근길에 무한 반복 중..")
        private String replyContent;

        @Schema(description = "보낸 시간", example = "2026-11-19T18:02:00")
        private LocalDateTime createdAt;
    }
}
