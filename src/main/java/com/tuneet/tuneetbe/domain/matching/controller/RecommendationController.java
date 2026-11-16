package com.tuneet.tuneetbe.domain.matching.controller;

import com.tuneet.tuneetbe.domain.matching.dto.request.RecommendationRequest;
import com.tuneet.tuneetbe.domain.matching.dto.response.RecommendationListResponse;
import com.tuneet.tuneetbe.domain.matching.service.RecommendationService;
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
@RequestMapping("/api/v1/users/me/recommendations")
@Tag(name = "Recommendation", description = "추천 사용자 API")
public class RecommendationController {

    private final RecommendationService recommendationService;

    // 추천 새로 받기
    @Operation(summary = "추천 사용자 새로 받기",
            description = "현재 위치를 저장하고 근처에서 취향이 비슷한 사용자를 최대 5명 다시 뽑는 API. 앱 접속, 새로고침 때 호출. "
                    + "위치 미동의면 U4007, 위치 노출 OFF면 U4013 (위치 유도 화면). "
                    + "본인, 탈퇴, 차단 관계, 위치 미동의·비공개, 7일 넘게 위치를 갱신하지 않은 사용자는 제외")
    @PostMapping
    public ResponseEntity<BaseResponse<RecommendationListResponse>> refresh(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RecommendationRequest request) {

        // service 호출
        RecommendationListResponse response = recommendationService.refresh(userDetails.getUserId(), request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "추천 사용자 갱신 성공", response));
    }

    // 추천 목록 조회
    @Operation(summary = "추천 사용자 조회",
            description = "마지막으로 받은 추천 사용자를 조회하는 API. 홈, 매칭 탭, 추천 목록이 같은 사용자를 보여 주도록 다시 뽑지 않음. "
                    + "추천을 받은 적이 없으면 빈 목록. 위치 미동의면 U4007, 위치 노출 OFF면 U4013")
    @GetMapping
    public ResponseEntity<BaseResponse<RecommendationListResponse>> getRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // service 호출
        RecommendationListResponse response = recommendationService.getRecommendations(userDetails.getUserId());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "추천 사용자 조회 성공", response));
    }
}
