package com.tuneet.tuneetbe.domain.music.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GenreType {

    @Schema(description = "K-POP")
    K_POP("K-POP"),

    @Schema(description = "POP")
    POP("POP"),

    @Schema(description = "HIP-HOP")
    HIP_HOP("HIP-HOP"),

    @Schema(description = "R&B / SOUL")
    R_AND_B_SOUL("R&B / SOUL"),

    @Schema(description = "BALLAD")
    BALLAD("BALLAD"),

    @Schema(description = "INDIE")
    INDIE("INDIE"),

    @Schema(description = "J-POP")
    J_POP("J-POP"),

    @Schema(description = "JAZZ")
    JAZZ("JAZZ"),

    @Schema(description = "EDM / ELECTRONIC")
    EDM_ELECTRONIC("EDM / ELECTRONIC"),

    @Schema(description = "ROCK")
    ROCK("ROCK"),

    @Schema(description = "OST")
    OST("OST");

    private final String displayName;
}
