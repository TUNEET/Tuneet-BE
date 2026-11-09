package com.tuneet.tuneetbe.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "회원가입 응답 dto", description = "회원가입 데이터를 보냈을때 서버에서 보내는 결과")
public class SignUpResponse {

    @Schema(description = "사용자 고유번호", example = "1")
    private Long userId;

    @Schema(description = "이메일", example = "test01@email.com")
    private String email;

    @Schema(description = "회원가입 날짜", example = "2026-11-07T10:40:00")
    private LocalDateTime createdAt;
}
