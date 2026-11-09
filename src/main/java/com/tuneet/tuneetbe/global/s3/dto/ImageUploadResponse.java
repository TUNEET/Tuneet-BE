package com.tuneet.tuneetbe.global.s3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "이미지 업로드 응답 dto")
public class ImageUploadResponse {

    @Schema(description = "업로드된 이미지 URL", example = "https://...")
    private String imageUrl;
}
