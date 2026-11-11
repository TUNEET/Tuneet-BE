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
@Schema(title = "방 목록 원소 dto", description = "나의 방 카드 1개")
public class RoomSummary {

    @Schema(description = "방 고유번호", example = "1")
    private Long roomId;

    @Schema(description = "방 사진 URL", example = "https://.../room/abc.png")
    private String roomPicture;

    @Schema(description = "방 이름", example = "퇴근길 플리 모임")
    private String roomTitle;

    @Schema(description = "방 소개", example = "퇴근길에 듣는 노래를 공유해요")
    private String roomDescription;

}
