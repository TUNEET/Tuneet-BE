package com.tuneet.tuneetbe.domain.room.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "방 입장 응답 dto", description = "방 입장 처리 후 서버가 보내는 결과. 새로 입장하면 201, 이미 멤버였으면 200")
public class RoomEntryResponse {

    @Schema(description = "입장한 방 고유번호", example = "1")
    private Long roomId;

    @Schema(description = "이미 참여 중인 방이었는지 (true면 비밀번호 확인 없이 바로 이동)", example = "false")
    private boolean alreadyJoined;

}
