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
@Schema(title = "피드 상세보기 조회 응답 dto", description = "사용자가 피드 하나를 조회할때 서버에서 보내는 결과")
public class FeedDetailResponse {

    @Schema(description = "피드 고유번호", example = "1")
    private Long feedId;

    @Schema(description = "내가 작성한 피드인지 여부", example = "true")
    private Boolean isMine;

    @Schema(description = "사용자 닉네임", example = "test01")
    private String nickname;

    @Schema(description = "사용자 프로필 사진 링크", example = "https://..")
    private String profilePicture;

    @Schema(description = "게시한 노래")
    private MusicResponse music;

    @Schema(description = "피드 내용", example = "요즘 출퇴근길마다 무한 반복 중...")
    private String content;

    @Schema(description = "피드 생성 시간", example = "2026-11-07T10:40:00")
    private LocalDateTime createdAt;
}
