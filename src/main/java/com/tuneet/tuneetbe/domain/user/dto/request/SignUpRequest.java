package com.tuneet.tuneetbe.domain.user.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "회원가입 dto", description = "사용자가 회원가입할때 요청 보내는 데이터")
public class SignUpRequest {

    @Schema(description = "이메일", example = "test01@email.com")
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 주소를 입력해 주세요.")
    private String email;

    @Schema(description = "비밀번호", example = "password12")
    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Pattern(
            regexp = "^(?![A-Za-z]+$)(?!\\d+$)(?![^A-Za-z\\d]+$)[A-Za-z\\d!@#$%^&*()\\-_=+\\[\\]{};:'\",.<>/?~`|\\\\]{8,16}$",
            message = "올바른 비밀번호를 입력해 주세요."
    )
    private String password;

    @Schema(description = "비밀번호 검증", example = "password12")
    @NotBlank(message = "비밀번호 확인은 필수 입력값입니다.")
    private String passwordConfirm;

    @Schema(description = "서비스 이용약관 동의 여부", example = "true")
    @AssertTrue(message = "필수 약관에 동의해 주세요.")
    private Boolean termsAgreed;

    @Schema(description = "개인정보 처리방침 동의 여부", example = "true")
    @AssertTrue(message = "필수 약관에 동의해 주세요.")
    private Boolean privacyAgreed;

    @Schema(description = "위치정보 수집 및 이용 동의 여부 (선택)", example = "false")
    private Boolean locationAgreed;
}
