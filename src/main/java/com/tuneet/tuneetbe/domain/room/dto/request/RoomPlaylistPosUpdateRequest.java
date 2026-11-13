package com.tuneet.tuneetbe.domain.room.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(title = "방 플레이리스트 위치 업데이트 요청 dto", description = "사용자가 플레이리스트 순서 수정 요청 보내는 데이터")
public class RoomPlaylistPosUpdateRequest {

    @Schema(description = "현재 플레이리스트의 roomPlaylistId 전체를 원하는 재생 순서대로", example = "[3, 1, 2]")
    @NotNull(message = "플레이리스트 순서는 필수 입력값입니다.")
    private List<Long> playListIdList;

}
