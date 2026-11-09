package com.tuneet.tuneetbe.domain.user.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum BlockType {

    @Schema(description = "차단")
    BLOCK,

    @Schema(description = "숨김")
    HIDE
}
