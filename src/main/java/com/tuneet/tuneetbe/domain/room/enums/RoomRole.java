package com.tuneet.tuneetbe.domain.room.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum RoomRole {

    @Schema(description = "방장")
    OWNER,

    @Schema(description = "멤버")
    MEMBER
}
