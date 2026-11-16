package com.tuneet.tuneetbe.domain.room.dto.response;

import com.tuneet.tuneetbe.domain.matching.service.TasteMatch;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.room.entity.RoomUser;
import com.tuneet.tuneetbe.domain.room.enums.RoomRole;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.enums.DistanceRange;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "멤버 정보 dto", description = "유저 리스트용 DTO")
public class RoomUserResponse {

    @Schema(description = "사용자 고유번호", example = "1")
    private Long userId;

    @Schema(description = "프로필 사진 URL", example = "https://.../profile/abc.png", nullable = true)
    private String profilePicture;

    @Schema(description = "닉네임", example = "튜닛")
    private String nickname;

    @Schema(description = "방 역할 (OWNER: 방장, MEMBER: 멤버)", example = "MEMBER")
    private RoomRole role;

    @Schema(description = "프로필 음악", nullable = true)
    private MusicResponse music;

    @Schema(description = "나와의 매칭률 (0~100). 내가 위치 미동의·위치 노출 OFF이거나 한쪽의 취향 데이터가 없으면 null", example = "87", nullable = true)
    private Integer matchPercent;

    // TODO 거리 표기
    // 1km 이내
    // 3km 이내
    // 5km 이내
    // 5km 이상 -> 서비스 규약상 이런식으로 해야함
    @Schema(description = "거리 구간. 나 또는 상대가 위치 미동의·위치 노출 OFF이거나 위치가 없거나 차단 관계면 null", example = "WITHIN_3KM", nullable = true)
    private DistanceRange distanceRange;

    public static RoomUserResponse from(RoomUser roomUser, TasteMatch match) {

        User user = roomUser.getUser();

        return RoomUserResponse.builder()
                .userId(user.getId())
                .profilePicture(user.getProfilePicture())
                .nickname(user.getNickname())
                .role(roomUser.getRole())
                .music(user.getProfileMusic() == null ? null : MusicResponse.from(user.getProfileMusic()))
                .matchPercent(match == null ? null : match.matchPercent())
                .distanceRange(match == null ? null : match.distanceRange())
                .build();
    }
}
