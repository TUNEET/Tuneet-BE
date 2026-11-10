package com.tuneet.tuneetbe.domain.letter.controller;

import com.tuneet.tuneetbe.domain.letter.dto.request.LetterSendRequest;
import com.tuneet.tuneetbe.domain.letter.dto.request.RandomLetterSendRequest;
import com.tuneet.tuneetbe.domain.letter.dto.response.LetterDetailResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.LetterListResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.LetterSendResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.RandomLetterSendResponse;
import com.tuneet.tuneetbe.domain.letter.service.LetterService;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.service.MusicCatalogService;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Letter", description = "편지 관련 API")
public class LetterController {

    private final LetterService letterService;
    private final MusicCatalogService musicCatalogService;

    // 지정 편지 발송
    @Operation(summary = "지정 편지 발송", description = "사용자가 수신자를 특정해 편지를 발송하는 API")
    @PostMapping("/letters")
    public ResponseEntity<BaseResponse<LetterSendResponse>> sendLetter(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LetterSendRequest request) {

        // 트랜잭션 밖에서 곡 확보
        Music music = musicCatalogService.getOrCreateMusics(List.of(request.getMusicId())).get(0);

        // service 호출
        LetterSendResponse response = letterService.sendLetter(userDetails.getUserId(), request.getReceiverId(), music, request.getLetterContent());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "지정 편지 발송 성공", response));
    }

    // 랜덤 편지 발송
    @Operation(summary = "랜덤 편지 발송", description = "사용자가 랜덤으로 선택된 다른 사용자에게 편지를 발송하는 API")
    @PostMapping("/letters/random")
    public ResponseEntity<BaseResponse<RandomLetterSendResponse>> sendRandomLetter(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RandomLetterSendRequest request) {

        // 트랜잭션 밖에서 곡 확보
        Music music = musicCatalogService.getOrCreateMusics(List.of(request.getMusicId())).get(0);

        // service 호출
        RandomLetterSendResponse response = letterService.sendRandomLetter(userDetails.getUserId(), music, request.getLetterContent());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "랜덤 편지 발송 성공", response));
    }

    // 편지 상세보기
    @Operation(summary = "편지 상세보기", description = "다른 사용자에게 편지를 받았을 때 편지 하나를 상세조회하는 API")
    @GetMapping("/letters/{letter-id}")
    public ResponseEntity<BaseResponse<LetterDetailResponse>> getLetter(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("letter-id") Long letterId) {

        // service 호출
        LetterDetailResponse response = letterService.letterDetail(userDetails.getUserId(), letterId);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "편지 상세보기 성공", response));
    }

    // 편지 리스트 보기
    @Operation(summary = "편지 리스트 보기", description = "사용자가 받은 편지 목록을 최신순으로 조회하는 API")
    @GetMapping("/letters")
    public ResponseEntity<BaseResponse<LetterListResponse>> getLetters(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {

        // service 호출
        LetterListResponse response = letterService.letterList(userDetails.getUserId(), cursor, size);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "편지 리스트 조회 성공", response));
    }
}
