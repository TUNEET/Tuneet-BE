package com.tuneet.tuneetbe.domain.auth.dto.reqeust;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "ReissueRequest: 토큰 재발급 요청 DTO")
public class ReissueRequest {

    @NotBlank(message = "Refresh Token은 필수입니다.")
    @Schema(description = "Refresh Token")
    private String refreshToken;
}
