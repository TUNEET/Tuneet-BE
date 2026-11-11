package com.tuneet.tuneetbe.domain.room.dto.response;

import com.tuneet.tuneetbe.domain.room.entity.RoomUser;
import com.tuneet.tuneetbe.domain.room.enums.RoomRole;
import com.tuneet.tuneetbe.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "멤버 요약 dto", description = "홈 화면에서 유저 간단히 표현하기 위한 DTO")
public class RoomUserSummaryResponse {

    @Schema(description = "사용자 고유번호", example = "1")
    private Long userId;

    @Schema(description = "프로필 사진 URL", example = "https://.../profile/abc.png", nullable = true)
    private String profilePicture;

    @Schema(description = "닉네임", example = "튜닛")
    private String nickname;

    @Schema(description = "방 역할 (OWNER: 방장, MEMBER: 멤버)", example = "MEMBER")
    private RoomRole role;

    public static RoomUserSummaryResponse from(RoomUser roomUser) {
        User user = roomUser.getUser();
        return RoomUserSummaryResponse.builder()
                .userId(user.getId())
                .profilePicture(user.getProfilePicture())
                .nickname(user.getNickname())
                .role(roomUser.getRole())
                .build();
    }
}
