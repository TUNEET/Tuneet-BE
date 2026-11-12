package com.tuneet.tuneetbe.domain.notification.controller;

import com.tuneet.tuneetbe.domain.notification.dto.request.DeviceTokenRequest;
import com.tuneet.tuneetbe.domain.notification.dto.response.NotificationListResponse;
import com.tuneet.tuneetbe.domain.notification.service.DeviceTokenService;
import com.tuneet.tuneetbe.domain.notification.service.NotificationService;
import com.tuneet.tuneetbe.global.common.BaseResponse;
import com.tuneet.tuneetbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Notification", description = "알림 관련 API")
public class NotificationController {

    private final NotificationService notificationService;
    private final DeviceTokenService deviceTokenService;

    // 알림 목록 조회
    @Operation(summary = "알림 목록 조회", description = "내 피드에 다른 사용자가 보낸 메시지 알림을 최신순으로 조회하는 API")
    @GetMapping("/notifications")
    public ResponseEntity<BaseResponse<NotificationListResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {

        // service 호출
        NotificationListResponse response = notificationService.getNotifications(userDetails.getUserId(), cursor, size);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "알림 목록 조회 성공", response));
    }

    // 알림 전체 삭제
    @Operation(summary = "알림 전체 삭제", description = "사용자가 받은 알림을 모두 삭제하는 API")
    @DeleteMapping("/notifications")
    public ResponseEntity<BaseResponse<Void>> deleteAllNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // service 호출
        notificationService.deleteNotifications(userDetails.getUserId());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "알림 전체 삭제 성공", null));
    }

    // FCM 토큰 등록
    @Operation(summary = "FCM 토큰 등록", description = "푸시 알림을 받기 위해 기기의 FCM 토큰을 등록하는 API")
    @PostMapping("/notifications/tokens")
    public ResponseEntity<BaseResponse<Void>> registerToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DeviceTokenRequest request) {

        // service 호출
        deviceTokenService.registerToken(userDetails.getUserId(), request.getToken());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "FCM 토큰 등록 성공", null));
    }

    // FCM 토큰 삭제
    @Operation(summary = "FCM 토큰 삭제", description = "로그아웃 전에 기기의 FCM 토큰을 삭제하는 API")
    @DeleteMapping("/notifications/tokens")
    public ResponseEntity<BaseResponse<Void>> deleteToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DeviceTokenRequest request) {

        // service 호출
        deviceTokenService.deleteToken(userDetails.getUserId(), request.getToken());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "FCM 토큰 삭제 성공", null));
    }
}
