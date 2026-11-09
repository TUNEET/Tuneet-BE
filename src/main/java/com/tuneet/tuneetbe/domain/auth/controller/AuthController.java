package com.tuneet.tuneetbe.domain.auth.controller;

import com.tuneet.tuneetbe.domain.auth.dto.reqeust.LoginRequest;
import com.tuneet.tuneetbe.domain.auth.dto.reqeust.ReissueRequest;
import com.tuneet.tuneetbe.domain.auth.dto.response.LoginResponse;
import com.tuneet.tuneetbe.domain.auth.service.AuthService;
import com.tuneet.tuneetbe.global.common.BaseResponse;
import com.tuneet.tuneetbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    // 로그인
    @Operation(summary = "로그인", description = "이메일과 비밀번호를 검증한 뒤 Access Token을 발급하는 API")
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        // service 호출
        LoginResponse response = authService.login(request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "로그인 성공", response));
    }

    // 토큰 재발급
    @Operation(summary = "토큰 재발급", description = "Refresh Token을 검증한 뒤 Access Token과 Refresh Token을 새로 발급하는 API")
    @PostMapping("/reissue")
    public ResponseEntity<BaseResponse<LoginResponse>> reissue(
            @Valid @RequestBody ReissueRequest request) {

        // service 호출
        LoginResponse response = authService.reissue(request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "토큰 재발급 성공", response));
    }

    // 로그아웃
    @Operation(summary = "로그아웃", description = "저장된 Refresh Token을 삭제하는 API")
    @DeleteMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // service 호출
        authService.logout(userDetails.getUserId());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "로그아웃 성공", null));
    }
}
