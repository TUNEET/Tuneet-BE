package com.tuneet.tuneetbe.domain.feed.controller;

import com.tuneet.tuneetbe.domain.feed.dto.request.FeedCreateRequest;
import com.tuneet.tuneetbe.domain.feed.dto.response.FeedCreateResponse;
import com.tuneet.tuneetbe.domain.feed.service.FeedService;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.service.MusicCatalogService;
import com.tuneet.tuneetbe.global.common.BaseResponse;
import com.tuneet.tuneetbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Feed", description = "피드 관련 API")
public class FeedController {

    private final FeedService feedService;
    private final MusicCatalogService musicCatalogService;

    @Operation(summary = "피드 생성", description = "사용자가 노래를 조회하고 등록후 피드 내용을 입력해 피드를 생성하는 API")
    @PostMapping("/feeds")
    public ResponseEntity<BaseResponse<FeedCreateResponse>> feedCreate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FeedCreateRequest request) {

        // 트랜잭션 밖에서 곡 확보
        Music music = musicCatalogService.getOrCreateMusics(List.of(request.getMusicId())).get(0);

        // service 호출
        FeedCreateResponse response = feedService.createFeed(userDetails.getUserId(), music, request.getContent());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "피드 작성 성공", response));
    }
}
