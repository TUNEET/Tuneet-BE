package com.tuneet.tuneetbe.domain.letter.controller;

import com.tuneet.tuneetbe.domain.letter.dto.request.LetterReplyRequest;
import com.tuneet.tuneetbe.domain.letter.dto.request.LetterSendRequest;
import com.tuneet.tuneetbe.domain.letter.dto.request.RandomLetterSendRequest;
import com.tuneet.tuneetbe.domain.letter.dto.response.LetterDetailResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.LetterListResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.LetterRemainingResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.LetterSendResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.RandomLetterSendResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.SentLetterListResponse;
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

    // 편지 단건 삭제
    @Operation(summary = "편지 단건 삭제", description = "사용자가 받은 편지를 편지함에서 삭제하는 API")
    @DeleteMapping("/letters/{letter-id}")
    public ResponseEntity<BaseResponse<Void>> deleteLetter(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("letter-id") Long letterId) {

        // service 호출
        letterService.deleteLetter(userDetails.getUserId(), letterId);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "편지 삭제 성공", null));
    }

    // 편지 전체 삭제
    @Operation(summary = "편지 전체 삭제", description = "사용자가 받은 편지를 편지함에서 모두 삭제하는 API")
    @DeleteMapping("/letters")
    public ResponseEntity<BaseResponse<Void>> deleteAllLetters(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // service 호출
        letterService.deleteAllLetters(userDetails.getUserId());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "편지 전체 삭제 성공", null));
    }

    // 오늘 남은 편지지 조회
    @Operation(summary = "오늘 남은 편지지 조회", description = "사용자가 오늘 보낼 수 있는 남은 편지 수를 조회하는 API")
    @GetMapping("/letters/remaining")
    public ResponseEntity<BaseResponse<LetterRemainingResponse>> getRemainingLetters(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // service 호출
        LetterRemainingResponse response = letterService.letterRemaining(userDetails.getUserId());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "오늘 남은 편지지 조회 성공", response));
    }

    // 편지 답장 보내기
    @Operation(summary = "편지 답장 보내기", description = "받은 편지를 보낸 사람에게 답장을 보내는 API")
    @PostMapping("/letters/{letter-id}/replies")
    public ResponseEntity<BaseResponse<LetterSendResponse>> letterReply(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("letter-id") Long letterId,
            @Valid @RequestBody LetterReplyRequest request) {

        // 트랜잭션 밖에서 곡 확보
        Music music = musicCatalogService.getOrCreateMusics(List.of(request.getMusicId())).get(0);

        // service 호출
        LetterSendResponse response = letterService.letterReply(userDetails.getUserId(), letterId, music, request.getLetterContent());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "답장 보내기 성공", response));
    }

    // 보낸 편지 리스트 보기
    @Operation(summary = "보낸 편지 리스트 보기", description = "사용자가 보낸 편지 목록을 최신순으로 조회하는 API")
    @GetMapping("/letters/sent")
    public ResponseEntity<BaseResponse<SentLetterListResponse>> getSentLetters(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {

        // service 호출
        SentLetterListResponse response = letterService.sentLetterList(userDetails.getUserId(), cursor, size);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "보낸 편지 리스트 조회 성공", response));
    }

    // 보낸 편지 전체 삭제
    @Operation(summary = "보낸 편지 전체 삭제", description = "사용자가 보낸 편지를 보낸 목록에서 모두 삭제하는 API (받은 사람의 편지함에는 그대로 남음)")
    @DeleteMapping("/letters/sent")
    public ResponseEntity<BaseResponse<Void>> deleteAllSentLetters(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // service 호출
        letterService.deleteAllSentLetters(userDetails.getUserId());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "보낸 편지 전체 삭제 성공", null));
    }
}
