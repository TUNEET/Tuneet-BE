package com.tuneet.tuneetbe.domain.platform.controller;

import com.tuneet.tuneetbe.domain.platform.dto.request.PlatformLinkRequest;
import com.tuneet.tuneetbe.domain.platform.dto.response.PlatformAuthorizeUrlResponse;
import com.tuneet.tuneetbe.domain.platform.dto.response.PlatformSyncResponse;
import com.tuneet.tuneetbe.domain.platform.service.YoutubeMusicLinkService;
import com.tuneet.tuneetbe.domain.platform.service.YoutubeMusicSyncService;
import com.tuneet.tuneetbe.global.common.BaseResponse;
import com.tuneet.tuneetbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/platforms/youtube-music")
@Tag(name = "Platform", description = "음원 플랫폼 연동 API")
public class PlatformController {

    private final YoutubeMusicLinkService youtubeMusicLinkService;
    private final YoutubeMusicSyncService youtubeMusicSyncService;

    // 연동 URL 발급
    @Operation(summary = "유튜브 뮤직 연동 URL 발급",
            description = "Google 동의 화면 주소를 발급하는 API. 이 주소로 이동하면 동의 후 프론트 콜백 페이지로 code와 state가 돌아온다. state는 10분 동안 유효")
    @GetMapping("/authorize-url")
    public ResponseEntity<BaseResponse<PlatformAuthorizeUrlResponse>> getAuthorizeUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // service 호출
        PlatformAuthorizeUrlResponse response = youtubeMusicLinkService.getAuthorizeUrl(userDetails.getUserId());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "유튜브 뮤직 연동 URL 발급 성공", response));
    }

    // 연동
    @Operation(summary = "유튜브 뮤직 연동",
            description = "콜백 페이지로 돌아온 code와 state로 연동하는 API. 이미 연동되어 있으면 토큰을 교체한다. 성공 후 좋아요 불러오기 API를 호출한다")
    @PostMapping
    public ResponseEntity<BaseResponse<Void>> link(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PlatformLinkRequest request) {

        // service 호출
        youtubeMusicLinkService.link(userDetails.getUserId(), request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "유튜브 뮤직 연동 성공", null));
    }

    // 좋아요 불러오기
    @Operation(summary = "유튜브 뮤직 좋아요 불러오기",
            description = "유튜브 뮤직 좋아요 곡(최근 1000개)을 불러와 저장하고 선호 아티스트 후보 3명을 응답하는 API. 유튜브에서 취소한 좋아요는 삭제된다. 좋아요가 많으면 몇 초 걸릴 수 있다")
    @PostMapping("/sync")
    public ResponseEntity<BaseResponse<PlatformSyncResponse>> sync(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // service 호출
        PlatformSyncResponse response = youtubeMusicSyncService.sync(userDetails.getUserId());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "유튜브 뮤직 좋아요 불러오기 성공", response));
    }

    // 연동 해제
    @Operation(summary = "유튜브 뮤직 연동 해제",
            description = "연동을 해제하고 유튜브에서 불러온 좋아요를 삭제하는 API. Google 토큰도 폐기한다")
    @DeleteMapping
    public ResponseEntity<BaseResponse<Void>> unlink(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // service 호출
        youtubeMusicLinkService.unlink(userDetails.getUserId());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "유튜브 뮤직 연동 해제 성공", null));
    }
}
