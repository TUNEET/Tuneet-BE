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
@Schema(title = "닉네임 사용 가능 여부 응답 dto", description = "프로필 추가 설정할때 닉네임을 중복인지 확인하면 서버에서 보내는 결과")
public class NicknameAvailabilityResponse {

    @Schema(description = "닉네임 사용 가능 여부 (true: 사용 가능, false: 이미 사용 중인 닉네임)", example = "true")
    private Boolean available;
}
