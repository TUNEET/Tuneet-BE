package com.tuneet.tuneetbe.domain.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "FCM 토큰 dto", description = "사용자가 FCM 기기 토큰을 등록, 삭제할때 요청 보내는 데이터")
public class DeviceTokenRequest {

    @Schema(description = "FCM 기기 토큰", example = "dXJmY2E6QVBBOTFi...")
    @NotBlank(message = "토큰은 필수 입력값입니다.")
    @Size(max = 500, message = "토큰은 500자 이하여야 합니다.")
    private String token;
}
