package com.tuneet.tuneetbe.domain.notification.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum NotificationType {

    @Schema(description = "피드 반응")
    REACTION,

    @Schema(description = "피드 코멘트")
    FEED_MESSAGE,

    @Schema(description = "편지")
    LETTER
}
