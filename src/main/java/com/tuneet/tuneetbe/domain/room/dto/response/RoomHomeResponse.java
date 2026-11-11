package com.tuneet.tuneetbe.domain.room.dto.response;

import com.tuneet.tuneetbe.domain.room.enums.RoomRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "방 홈 화면 dto", description = "방 홈 화면 응답 DTO")
public class RoomHomeResponse {

    @Schema(description = "방 고유번호", example = "1")
    private Long roomId;

    @Schema(description = "방 이름", example = "퇴근길 플리 모임")
    private String roomTitle;

    @Schema(description = "방 소개", example = "퇴근길에 듣는 노래를 공유해요")
    private String roomDescription;

    @Schema(description = "방 사진 URL", example = "https://.../room/abc.png")
    private String roomPicture;

    @Schema(description = "초대 코드 (초대 링크는 프론트에서 tuneet.com/invite/{inviteCode}로 조합)", example = "aB3kP9xQ")
    private String inviteCode;

    @Schema(description = "비밀번호 설정 여부", example = "true")
    private boolean hasPassword;

    @Schema(description = "내 역할 (OWNER: 방장, MEMBER: 멤버)", example = "OWNER")
    private RoomRole myRole;

    @Schema(description = "방 전체 멤버 수", example = "5")
    private int memberCount;

    @Builder.Default
    @Schema(description = "멤버 목록 (방장 먼저, 나머지는 입장 순)")
    private List<RoomUserSummaryResponse> roomUsers = new ArrayList<>();

    @Builder.Default
    @Schema(description = "최근 포스트 (최신순 최대 2개)")
    private List<RoomPostResponse> roomPosts = new ArrayList<>();

    @Builder.Default
    @Schema(description = "플레이리스트 앞부분 (재생 순서대로 최대 2곡)")
    private List<RoomPlaylistResponse> roomPlaylists = new ArrayList<>();
}
