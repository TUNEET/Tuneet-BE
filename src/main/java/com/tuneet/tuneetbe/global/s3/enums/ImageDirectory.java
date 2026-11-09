package com.tuneet.tuneetbe.global.s3.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageDirectory {

    @Schema(description = "방")
    ROOM("room"),

    @Schema(description = "프로필 사진")
    PROFILE("profile-image"),;

    private final String path;
}
