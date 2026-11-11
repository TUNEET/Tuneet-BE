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
@Schema(title = "방 입장 요청 dto", description = "사용자가 방 입장 요청시 보내는 데이터")
public class RoomEntryRequest {

    @Schema(description = "초대 코드 (초대 링크 tuneet.com/invite/{inviteCode}의 마지막 부분)", example = "aB3kP9xQ")
    @NotBlank(message = "초대 코드는 필수 입력값입니다.")
    @Size(max = 20, message = "올바른 초대 코드가 아닙니다.")
    private String inviteCode;

    @Schema(description = "방 비밀번호 (잠긴 방만, 숫자 4자리. 공개 방이면 null)", example = "1234", nullable = true)
    @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 숫자 4자리여야 합니다.")
    private String password;

}
