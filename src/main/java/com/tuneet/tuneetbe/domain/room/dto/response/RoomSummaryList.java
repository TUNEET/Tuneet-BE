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
@Schema(title = "방 목록 응답 dto", description = "내가 참여한 방 목록 (최근 참여순)")
public class RoomSummaryList {

    @Schema(description = "방 목록")
    @Builder.Default
    private List<RoomSummary> rooms = new ArrayList<>();

    public void addRoomSummaryList(RoomSummary roomSummary) {
        this.rooms.add(roomSummary);
    }

}
