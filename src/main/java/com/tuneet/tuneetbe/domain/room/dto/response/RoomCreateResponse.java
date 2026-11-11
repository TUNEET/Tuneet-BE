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
@Schema(title = "방 생성 응답 dto", description = "방 생성 후 서버가 보내는 결과. 초대 링크는 프론트에서 tuneet.com/invite/{inviteCode}로 조합")
public class RoomCreateResponse {

    @Schema(description = "방 고유번호", example = "1")
    private Long roomId;

    @Schema(description = "방 사진 URL", example = "https://.../room/abc.png")
    private String roomPicture;

    @Schema(description = "방 이름", example = "퇴근길 플리 모임")
    private String roomTitle;

    @Schema(description = "방 소개", example = "퇴근길에 듣는 노래를 공유해요")
    private String roomDescription;

    @Schema(description = "초대 코드 (8자리)", example = "aB3kP9xQ")
    private String inviteCode;

    @Schema(description = "비밀번호 설정 여부", example = "true")
    private Boolean hasPassword;


}
