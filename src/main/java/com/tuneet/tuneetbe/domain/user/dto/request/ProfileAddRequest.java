package com.tuneet.tuneetbe.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "프로필 추가 설정 dto", description = "사용자가 회원가입후 로그인을 하고 프로필 추가 설정할 때 요청 보내는 데이터")
public class ProfileAddRequest {

    @Schema(description = "닉네임", example = "test01")
    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
    private String nickname;

    @Schema(description = "프로필 사진 url", example = "https://...")
    private String profilePicture;
}
