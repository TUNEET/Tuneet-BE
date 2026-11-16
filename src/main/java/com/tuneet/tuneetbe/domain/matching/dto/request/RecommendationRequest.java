package com.tuneet.tuneetbe.domain.matching.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "추천 사용자 새로 받기 dto", description = "추천 기준이 되는 현재 위치 (서버에 저장되고, 다른 사용자에게는 거리 구간으로만 보임)")
public class RecommendationRequest {

    @Schema(description = "위도", example = "37.5665")
    @NotNull(message = "위도는 필수 입력값입니다.")
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90 이하이어야 합니다.")
    private Double latitude;

    @Schema(description = "경도", example = "126.9780")
    @NotNull(message = "경도는 필수 입력값입니다.")
    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180 이하이어야 합니다.")
    private Double longitude;
}
