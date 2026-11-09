package com.tuneet.tuneetbe.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "설정 변경 dto", description = "사용자가 설정을 변경할 때 요청 보내는 데이터")
public class SettingsUpdateRequest {

    @Schema(description = "위치정보 노출 여부", example = "true")
    private Boolean locationVisible;

    @Schema(description = "편지 받기 여부", example = "true")
    private Boolean letterReceivable;

    @Schema(description = "위치정보 수집 및 이용 동의 여부", example = "true")
    private Boolean locationAgreed;
}
