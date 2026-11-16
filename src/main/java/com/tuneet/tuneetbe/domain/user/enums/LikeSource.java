package com.tuneet.tuneetbe.domain.user.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum LikeSource {

    @Schema(description = "앱에서 누른 좋아요")
    IN_APP,

    @Schema(description = "유튜브 뮤직에서 불러온 좋아요")
    YOUTUBE_MUSIC
}
