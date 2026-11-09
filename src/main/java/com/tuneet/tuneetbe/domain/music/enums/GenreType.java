package com.tuneet.tuneetbe.domain.music.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum GenreType {

    @Schema(description = "K-POP")
    K_POP,

    @Schema(description = "POP")
    POP,

    @Schema(description = "HIP-HOP")
    HIP_HOP,

    @Schema(description = "R&B / SOUL")
    R_AND_B_SOUL,

    @Schema(description = "BALLAD")
    BALLAD,

    @Schema(description = "INDIE")
    INDIE,

    @Schema(description = "J-POP")
    J_POP,

    @Schema(description = "JAZZ")
    JAZZ,

    @Schema(description = "EDM / ELECTRONIC")
    EDM_ELECTRONIC,

    @Schema(description = "ROCK")
    ROCK,

    @Schema(description = "OST")
    OST
}
