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
@Schema(title = "편지 상세보기 응답 dto", description = "다른 사용자에게 편지를 받았을 때 서버에서 보내는 결과")
public class LetterDetailResponse {

    @Schema(description = "편지 고유번호", example = "1")
    private Long letterId;

    @Schema(description = "보낸 사람 고유번호", example = "1")
    private Long senderId;

    @Schema(description = "보낸 사람 닉네임 (탈퇴했으면 -> 탈퇴한 사용자)", example = "test01")
    private String senderNickname;

    @Schema(description = "편지에서 보낸 노래")
    private MusicResponse music;

    @Schema(description = "편지 내용", example = "오늘도 충분히 예쁘고 멋진 하루 보내길 바라!")
    private String letterContent;

    @Schema(description = "읽음 여부", example = "false")
    private Boolean isRead;

    @Schema(description = "편지 생성 시간", example = "2026-11-07T10:40:00")
    private LocalDateTime createdAt;

    @Schema(description = "답장 여부", example = "false")
    private Boolean isReply;

    @Schema(description = "내가 이 편지에 이미 답장했는지 여부 (true면 답장하기 버튼 숨김, 목록 조회면 null)", example = "false")
    private Boolean isReplied;

    @Schema(description = "답장 대상인 바로 이전 편지 (답장이 아니거나 목록 조회면 null)")
    private ParentLetter parentLetter;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "답장 대상 편지 응답 dto", description = "답장 상세에서 보여줄 바로 이전 편지")
    public static class ParentLetter {

        @Schema(description = "편지 고유번호", example = "10")
        private Long letterId;

        @Schema(description = "편지 내용", example = "오늘 하루 힘내!")
        private String letterContent;

        @Schema(description = "편지에서 보낸 노래")
        private MusicResponse music;

        @Schema(description = "보낸 시간", example = "2026-11-11T09:38:00")
        private LocalDateTime createdAt;
    }
}
