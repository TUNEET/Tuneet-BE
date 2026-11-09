package com.tuneet.tuneetbe.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "설정 응답 dto", description = "사용자의 설정을 조회하거나 변경했을때 서버에서 보내는 결과")
public class SettingsResponse {

    @Schema(description = "위치정보 노출 여부", example = "true")
    private Boolean locationVisible;

    @Schema(description = "편지 받기 여부", example = "true")
    private Boolean letterReceivable;

    @Schema(description = "음원 플랫폼 연동 여부", example = "false")
    private Boolean platformConnected;

    @Schema(description = "서비스 이용약관 동의 여부", example = "true")
    private Boolean termsAgreed;

    @Schema(description = "개인정보 처리방침 동의 여부", example = "true")
    private Boolean privacyAgreed;

    @Schema(description = "위치정보 수집 및 이용 동의 여부", example = "true")
    private Boolean locationAgreed;
}
