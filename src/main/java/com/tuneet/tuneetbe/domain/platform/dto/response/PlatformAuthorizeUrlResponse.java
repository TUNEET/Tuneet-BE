package com.tuneet.tuneetbe.domain.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "유튜브 뮤직 연동 URL 응답 dto", description = "이 주소로 이동하면 Google 동의 화면이 열림")
public class PlatformAuthorizeUrlResponse {

    @Schema(description = "Google 동의 화면 주소", example = "https://accounts.google.com/o/oauth2/v2/auth?client_id=...")
    private String url;
}
