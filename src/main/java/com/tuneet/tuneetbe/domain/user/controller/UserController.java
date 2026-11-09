package com.tuneet.tuneetbe.domain.user.controller;

import com.tuneet.tuneetbe.domain.user.dto.request.ProfileAddRequest;
import com.tuneet.tuneetbe.domain.user.dto.request.SignUpRequest;
import com.tuneet.tuneetbe.domain.user.dto.response.EmailAvailabilityResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.ProfileAddResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.SignUpResponse;
import com.tuneet.tuneetbe.domain.user.service.UserService;
import com.tuneet.tuneetbe.global.common.BaseResponse;
import com.tuneet.tuneetbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;

    // 회원가입
    @Operation(summary = "회원가입", description = "이메일, 비밀번호를 입력받아 사용자를 생성하는 API")
    @PostMapping("/users")
    public ResponseEntity<BaseResponse<SignUpResponse>> signUp(
            @Valid @RequestBody SignUpRequest request) {

        // service 호출
        SignUpResponse response = userService.signUp(request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "회원가입 성공", response));
    }

    // 이메일 사용 가능 여부 확인
    @Operation(summary = "이메일 사용 가능 여부 확인", description = "사용자가 회원가입할때 이메일 사용 가능 여부를 확인하는 API")
    @GetMapping("/users/email/availability")
    public ResponseEntity<BaseResponse<EmailAvailabilityResponse>> emailAvailability(
            @RequestParam
            @NotBlank(message = "이메일은 필수 입력값입니다.")
            @Email(message = "올바른 이메일 주소를 입력해 주세요.")
            String email) {

        // service 호출
        EmailAvailabilityResponse response = userService.emailAvailability(email);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "이메일 사용 가능 여부 확인 성공", response));
    }

    // 프로필 추가 설정
    @Operation(summary = "프로필 추가 설정", description = "사용자가 로그인 후 프로필 추가 설정할때 사용하는 API")
    @PatchMapping("/users/me")
    public ResponseEntity<BaseResponse<ProfileAddResponse>> profileAdd(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProfileAddRequest request) {

        // service 호출
        ProfileAddResponse response = userService.profileAdd(userDetails.getUserId(), request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "프로필 추가 설정 성공", response));
    }
}
