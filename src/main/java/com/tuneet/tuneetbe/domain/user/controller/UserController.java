package com.tuneet.tuneetbe.domain.user.controller;

import com.tuneet.tuneetbe.domain.user.dto.request.GenreReplaceRequest;
import com.tuneet.tuneetbe.domain.user.dto.request.ProfileAddRequest;
import com.tuneet.tuneetbe.domain.user.dto.request.SettingsUpdateRequest;
import com.tuneet.tuneetbe.domain.user.dto.request.SignUpRequest;
import com.tuneet.tuneetbe.domain.user.dto.response.EmailAvailabilityResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.GenreReplaceResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.MyProfileResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.NicknameAvailabilityResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.ProfileAddResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.SettingsResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.SignUpResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.UserProfileResponse;
import com.tuneet.tuneetbe.domain.user.service.UserService;
import com.tuneet.tuneetbe.global.common.BaseResponse;
import com.tuneet.tuneetbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    // 닉네임 사용 가능 여부 확인
    @Operation(summary = "닉네임 사용 가능 여부 확인", description = "사용자가 프로필 추가 설정할때 닉네임 사용 가능 여부를 확인하는 API")
    @GetMapping("/users/nickname/availability")
    public ResponseEntity<BaseResponse<NicknameAvailabilityResponse>> nicknameAvailability(
            @RequestParam
            @NotBlank(message = "닉네임은 필수 입력값입니다.")
            @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
            String nickname) {

        // service 호출
        NicknameAvailabilityResponse response = userService.nicknameAvailability(nickname);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "닉네임 사용 가능 여부 확인 성공", response));
    }

    // 내 정보 조회
    @Operation(summary = "내 정보 조회", description = "로그인한 사용자가 마이 화면에서 본인의 프로필을 조회하는 API")
    @GetMapping("/users")
    public ResponseEntity<BaseResponse<MyProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // service 호출
        MyProfileResponse response = userService.getMyProfile(userDetails.getUserId());

        // 응답 반혼
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "내 정보 조회 성공", response));
    }

    // 선호 장르 교체
    @Operation(summary = "선호 장르 교체", description = "로그인한 사용자의 선호 장르를 요청한 목록으로 교체하는 API")
    @PutMapping("/users/me/genres")
    public ResponseEntity<BaseResponse<GenreReplaceResponse>> replaceGenres(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GenreReplaceRequest request) {

        // service 호출
        GenreReplaceResponse response = userService.replaceGenres(userDetails.getUserId(), request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "선호 장르 교체 성공", response));
    }

    // 설정 조회
    @Operation(summary = "설정 조회", description = "로그인한 사용자가 설정 화면에서 현재 설정을 조회하는 API")
    @GetMapping("/users/me/settings")
    public ResponseEntity<BaseResponse<SettingsResponse>> getSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // service 호출
        SettingsResponse response = userService.getSettings(userDetails.getUserId());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "설정 조회 성공", response));
    }

    // 설정 변경
    @Operation(summary = "설정 변경", description = "위치정보 노출, 편지 받기, 위치정보 동의를 변경하는 API")
    @PatchMapping("/users/me/settings")
    public ResponseEntity<BaseResponse<SettingsResponse>> updateSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SettingsUpdateRequest request) {

        // service 호출
        SettingsResponse response = userService.updateSettings(userDetails.getUserId(), request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "설정 변경 성공", response));
    }

    // 다른 사용자 프로필 조회
    @Operation(summary = "다른 사용자 프로필 조회", description = "로그인한 사용자가 다른 사용자의 프로필과 함께 좋아하는 항목을 조회하는 API")
    @GetMapping("/users/{user-id}")
    public ResponseEntity<BaseResponse<UserProfileResponse>> getUserProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("user-id") Long userId) {

        // service 호출
        UserProfileResponse response = userService.getUserProfile(userDetails.getUserId(), userId);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "다른 사용자 프로필 조회 성공", response));
    }
}
