package com.tuneet.tuneetbe.domain.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "유튜브 뮤직 연동 dto", description = "Google 동의 후 콜백 페이지로 돌아온 code와 state를 그대로 보내는 데이터")
public class PlatformLinkRequest {

    @Schema(description = "콜백 주소의 code (한 번만 사용 가능)", example = "4/0AeanS0b...")
    @NotBlank(message = "code는 필수 입력값입니다.")
    private String code;

    @Schema(description = "콜백 주소의 state (연동 URL 발급 때 서버가 넣은 값)", example = "MTo...")
    @NotBlank(message = "state는 필수 입력값입니다.")
    private String state;
}
