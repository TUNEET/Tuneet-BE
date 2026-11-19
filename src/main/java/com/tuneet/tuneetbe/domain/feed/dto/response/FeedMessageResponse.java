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
@Schema(title = "피드 코멘트 응답 dto", description = "피드에 달린 코멘트 하나의 정보")
public class FeedMessageResponse {

    @Schema(description = "보낸 사람 닉네임 (탈퇴했으면 -> 탈퇴한 사용자)", example = "노래하는 튜니")
    private String senderNickname;

    @Schema(description = "보낸 사람 프로필 사진 (없거나 탈퇴했으면 null)", example = "https://example.com/profile.png")
    private String senderProfilePicture;

    @Schema(description = "코멘트와 함께 보낸 노래 (없으면 null)")
    private MusicResponse music;

    @Schema(description = "코멘트 내용", example = "요즘 출퇴근길마다 무한 반복 중..")
    private String messageContent;

    @Schema(description = "보낸 시간", example = "2026-11-19T18:02:00")
    private LocalDateTime createdAt;
}
