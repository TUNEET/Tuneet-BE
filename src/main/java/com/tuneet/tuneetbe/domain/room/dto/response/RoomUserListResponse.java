package com.tuneet.tuneetbe.domain.room.dto.response;

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
@Schema(title = "유저 목록 응답 dto", description = "현재 방의 유저 목록")
public class RoomUserListResponse {

    @Schema(description = "방 고유번호", example = "1")
    private Long roomId;

    @Schema(description = "방 이름", example = "퇴근길 플리 모임")
    private String roomTitle;

    @Schema(description = "방 소개", example = "퇴근길에 듣는 노래를 공유해요")
    private String roomDescription;

    @Schema(description = "유저 목록")
    @Builder.Default
    private List<RoomUserResponse> roomUsers = new ArrayList<>();

}
