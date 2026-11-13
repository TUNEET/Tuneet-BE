package com.tuneet.tuneetbe.domain.user.dto.response;

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
@Schema(title = "차단한 사용자 목록 조회 응답 dto", description = "사용자가 차단한 사용자 목록을 조회할때 서버에서 보내는 결과")
public class BlockedUserListResponse {

    @Schema(description = "차단한 사용자 목록 (최근 차단순)")
    private List<BlockedUser> blockedUsers;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "차단한 사용자 응답 dto", description = "차단한 사용자 목록에서 사용자 한 명의 정보")
    public static class BlockedUser {

        @Schema(description = "차단한 사용자 고유번호 (차단 해제할 때 사용)", example = "2")
        private Long userId;

        @Schema(description = "차단한 사용자 닉네임", example = "산책하는 튜니")
        private String nickname;

        @Schema(description = "차단한 사용자 프로필 사진", example = "https://..")
        private String profilePicture;

        @Schema(description = "차단한 날짜", example = "2026-11-21T10:40:00")
        private LocalDateTime blockedAt;
    }
}
