package com.tuneet.tuneetbe.domain.user.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum DistanceRange {

    @Schema(description = "1km 이내")
    WITHIN_1KM,

    @Schema(description = "3km 이내")
    WITHIN_3KM,

    @Schema(description = "5km 이내")
    WITHIN_5KM,

    @Schema(description = "5km 이상")
    OVER_5KM
}
