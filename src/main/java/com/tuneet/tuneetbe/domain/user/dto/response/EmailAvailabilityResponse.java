package com.tuneet.tuneetbe.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "이메일 사용 가능 여부 응답 dto", description = "회원가입할때 이메일을 중복인지 확인하면 서버에서 보내는 결과")
public class EmailAvailabilityResponse {

    @Schema(description = "이메일 중복 여부", example = "true")
    private Boolean available;
}
