package com.tuneet.tuneetbe.domain.room.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(title = "방 생성 요청 dto", description = "사용자가 방을 만들 때 보내는 데이터")
public class RoomCreateRequest {

    /*
    방 만들때 필요한 정보 목록
    1. 방 이름
    2. 방 소개
    3. 방 사진 (선택)
    4. 비밀번호 (선택, 없으면 null)
     */

    @Schema(description = "방 이름 (최대 50자)", example = "퇴근길 플리 모임")
    @NotBlank(message = "방 이름은 필수 입력값입니다.")
    @Size(max = 50, message = "방 이름은 50자 이하여야 합니다.")
    private String roomTitle;

    @Schema(description = "방 소개 (선택, 최대 200자)", example = "퇴근길에 듣는 노래를 공유해요")
    @Size(max = 200, message = "방 소개는 200자 이하여야 합니다.")
    private String roomDescription;

    @Schema(description = "방 사진 URL (선택, 이미지 업로드 API로 먼저 올린 주소)", example = "https://.../room/abc.png")
    @Size(max = 300, message = "방 사진 URL이 너무 깁니다.")
    private String roomPicture;

    @Schema(description = "방 비밀번호 (선택, 숫자 4자리, 없으면 null)", example = "1234", nullable = true)
    @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 숫자 4자리여야 합니다.")
    private String password;


}
