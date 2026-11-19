package com.tuneet.tuneetbe.domain.feed.controller;

import com.tuneet.tuneetbe.domain.feed.dto.request.FeedCreateRequest;
import com.tuneet.tuneetbe.domain.feed.dto.request.FeedMessageReplyRequest;
import com.tuneet.tuneetbe.domain.feed.dto.request.FeedSendMessageRequest;
import com.tuneet.tuneetbe.domain.feed.dto.request.FeedUpdateRequest;
import com.tuneet.tuneetbe.domain.feed.dto.response.*;
import com.tuneet.tuneetbe.domain.feed.entity.FeedMessage;
import com.tuneet.tuneetbe.domain.feed.service.FeedMessageReplyService;
import com.tuneet.tuneetbe.domain.feed.service.FeedService;
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
@Tag(name = "Feed", description = "피드 관련 API")
public class FeedController {

    private final FeedService feedService;
    private final FeedMessageReplyService feedMessageReplyService;
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

    // 피드 상세보기
    @Operation(summary = "피드 상세보기", description = "사용자가 피드 1개를 조회할때 사용하는 API")
    @GetMapping("/feeds/{feed-id}")
    public ResponseEntity<BaseResponse<FeedDetailResponse>> feedDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("feed-id") Long feedId) {

        // service 호출
        FeedDetailResponse response = feedService.getFeed(userDetails.getUserId(), feedId);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "피드 상세보기 조회 성공", response));
    }

    // 피드 리스트 보기
    @Operation(summary = "피드 리스트 보기", description = "사용자가 피드 목록을 최신순으로 조회할때 사용하는 API")
    @GetMapping("/feeds")
    public ResponseEntity<BaseResponse<FeedListResponse>> feedList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {

        // service 호출
        FeedListResponse response = feedService.getFeeds(userDetails.getUserId(), cursor, size);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "피드 리스트 조회 성공", response));
    }

    // 피드 수정
    @Operation(summary = "피드 수정", description = "사용자가 피드를 수정하는 API")
    @PutMapping("/feeds/{feed-id}")
    public ResponseEntity<BaseResponse<FeedUpdateResponse>> feedUpdate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("feed-id") Long feedId,
            @Valid @RequestBody FeedUpdateRequest request) {

        // 노래를 바꿀 때만 트랜잭션 밖에서 곡 확보
        Music music = null;
        if (request.getMusicId() != null) {
            music = musicCatalogService.getOrCreateMusics(List.of(request.getMusicId())).get(0);
        }

        // service 호출
        FeedUpdateResponse response = feedService.updateFeed(userDetails.getUserId(), music, request.getContent(), feedId);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "피드 수정 성공", response));
    }

    // 피드 삭제
    @Operation(summary = "피드 삭제", description = "사용자가 본인이 작성한 피드를 삭제하는 API")
    @DeleteMapping("/feeds/{feed-id}")
    public ResponseEntity<BaseResponse<Void>> deleteFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("feed-id") Long feedId) {

        // service 호출
        feedService.deleteFeed(userDetails.getUserId(), feedId);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "피드 삭제 성공", null));
    }

    // 피드 메시지 작성
    @Operation(summary = "피드 메시지 작성", description = "사용자가 피드를 보고 메시지를 작성할때 사용하는 API")
    @PostMapping("/feeds/{feed-id}/messages")
    public ResponseEntity<BaseResponse<FeedSendMessageResponse>> feedSendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FeedSendMessageRequest request,
            @PathVariable("feed-id") Long feedId) {

        // 트랜잭션 밖에서 곡 확보
        Music music = musicCatalogService.getOrCreateMusics(List.of(request.getMusicId())).get(0);

        // service 호출
        FeedSendMessageResponse response = feedService.sendMessage(userDetails.getUserId(), feedId, request.getMessageContent(), music);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "피드 메시지 작성 성공", response));
    }

    // 피드 코멘트 목록 보기
    @Operation(summary = "피드 코멘트 목록 보기", description = "피드 하나에 달린 코멘트 전체를 최신순으로 조회하는 API (차단 관계인 사용자의 코멘트는 제외)")
    @GetMapping("/feeds/{feed-id}/messages")
    public ResponseEntity<BaseResponse<FeedMessageListResponse>> getFeedMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("feed-id") Long feedId) {

        // service 호출
        FeedMessageListResponse response = feedService.getFeedMessages(userDetails.getUserId(), feedId);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "피드 코멘트 목록 조회 성공", response));
    }

    // 코멘트 답장 보내기
    @Operation(summary = "코멘트 답장 보내기", description = "내 피드에 받은 코멘트에 답장을 보내는 API (코멘트당 한 번)")
    @PostMapping("/feeds/messages/{feed-message-id}/replies")
    public ResponseEntity<BaseResponse<FeedMessageReplySendResponse>> replyToMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("feed-message-id") Long feedMessageId,
            @Valid @RequestBody FeedMessageReplyRequest request) {

        // 트랜잭션 밖에서 곡 확보
        Music music = musicCatalogService.getOrCreateMusics(List.of(request.getMusicId())).get(0);

        // service 호출
        FeedMessageReplySendResponse response = feedMessageReplyService.replyToMessage(userDetails.getUserId(), feedMessageId, music, request.getReplyContent());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "코멘트 답장 보내기 성공", response));
    }

    // 받은 답장에 다시 답장 보내기
    @Operation(summary = "받은 답장에 답장 보내기", description = "받은 코멘트 답장에 다시 답장을 보내는 API (답장당 한 번, 계속 주고받을 수 있음)")
    @PostMapping("/feeds/messages/replies/{reply-id}/replies")
    public ResponseEntity<BaseResponse<FeedMessageReplySendResponse>> replyToReply(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("reply-id") Long replyId,
            @Valid @RequestBody FeedMessageReplyRequest request) {

        // 트랜잭션 밖에서 곡 확보
        Music music = musicCatalogService.getOrCreateMusics(List.of(request.getMusicId())).get(0);

        // service 호출
        FeedMessageReplySendResponse response = feedMessageReplyService.replyToReply(userDetails.getUserId(), replyId, music, request.getReplyContent());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "답장 보내기 성공", response));
    }

    // 받은 답장 상세보기
    @Operation(summary = "받은 답장 상세보기", description = "답장 알림을 눌렀을 때 받은 답장 하나를 상세조회하는 API")
    @GetMapping("/feeds/messages/replies/{reply-id}")
    public ResponseEntity<BaseResponse<FeedMessageReplyDetailResponse>> getReply(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("reply-id") Long replyId) {

        // service 호출
        FeedMessageReplyDetailResponse response = feedMessageReplyService.getReply(userDetails.getUserId(), replyId);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "답장 상세보기 성공", response));
    }

    // 보낸 코멘트 리스트 보기
    @Operation(summary = "보낸 코멘트 리스트 보기", description = "사용자가 다른 사용자의 피드에 보낸 메시지 목록을 최신순으로 조회하는 API")
    @GetMapping("/feeds/messages/sent")
    public ResponseEntity<BaseResponse<SentFeedMessageListResponse>> getSentMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {

        // service 호출
        SentFeedMessageListResponse response = feedService.getSentMessages(userDetails.getUserId(), cursor, size);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "보낸 코멘트 리스트 조회 성공", response));
    }

    // 보낸 코멘트 전체 삭제
    @Operation(summary = "보낸 코멘트 전체 삭제", description = "사용자가 보낸 메시지를 보낸 목록에서 모두 삭제하는 API (받은 사람의 알림에는 그대로 남음)")
    @DeleteMapping("/feeds/messages/sent")
    public ResponseEntity<BaseResponse<Void>> deleteAllSentMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // service 호출
        feedService.deleteAllSentMessages(userDetails.getUserId());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "보낸 코멘트 전체 삭제 성공", null));
    }
}
