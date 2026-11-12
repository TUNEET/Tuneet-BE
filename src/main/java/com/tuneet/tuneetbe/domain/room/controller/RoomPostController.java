package com.tuneet.tuneetbe.domain.room.controller;

import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.service.MusicCatalogService;
import com.tuneet.tuneetbe.domain.room.dto.request.RoomPostCreateRequest;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomPostListResponse;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomPostResponse;
import com.tuneet.tuneetbe.domain.room.service.RoomPostService;
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
@RequestMapping("/api/v1/rooms")
@Tag(name = "Room", description = "방 API")
public class RoomPostController {

    private final RoomPostService roomPostService;

    private final MusicCatalogService musicCatalogService;

    @Operation(summary = "방 포스트 작성", description = "방에 노래와 코멘트(최대 100자)를 함께 올리는 API. 오류: G001 입력값 오류, M4001 존재하지 않는 노래, M5001 음악 서버 통신 실패, U4002 존재하지 않는 사용자, R4001 존재하지 않는 방, R4004 방 멤버 아님")
    @PostMapping("/{room-id}/posts")
    public ResponseEntity<BaseResponse<RoomPostResponse>> createRoomPost(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                         @Valid @RequestBody RoomPostCreateRequest request,
                                                                         @PathVariable("room-id") Long roomId){

        Music music = musicCatalogService.getOrCreateMusics(List.of(request.getMusicId())).get(0);

        RoomPostResponse response = roomPostService.createRoomPost(roomId, customUserDetails.getUserId(), music, request.getContent());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(201, "방 포스트 작성 성공", response));

    }

    @Operation(summary = "방 포스트 목록 조회", description = "방 포스트를 최신순으로 조회하는 API. 첫 요청은 cursor 없이, 다음 요청부터는 직전 응답의 마지막 roomPostId를 cursor로 보내고 hasNext가 false면 멈춤. 오류: G001 size가 1~50 범위 밖, R4001 존재하지 않는 방, R4004 방 멤버 아님")
    @GetMapping("/{room-id}/posts")
    public ResponseEntity<BaseResponse<RoomPostListResponse>> getRoomPostList(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                              @PathVariable("room-id") Long roomId,
                                                                              @RequestParam(required = false) Long cursor,
                                                                              @RequestParam(defaultValue = "20") @Min(1) @Max(50) Integer size){

        RoomPostListResponse response = roomPostService.getRoomPostList(roomId, customUserDetails.getUserId(), cursor, size);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "방 포스트 목록 조회 성공", response));

    }

    @Operation(summary = "방 포스트 삭제", description = "내가 작성한 방 포스트를 삭제하는 API. 오류: R4001 존재하지 않는 방, R4004 방 멤버 아님, R4005 포스트가 없거나 다른 방의 포스트, R4006 작성자가 아님")
    @DeleteMapping("/{room-id}/posts/{post-id}")
    public ResponseEntity<BaseResponse<Void>> deleteRoomPost(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                             @PathVariable("room-id") Long roomId,
                                                             @PathVariable("post-id") Long postId){

        roomPostService.deleteRoomPost(roomId, postId, customUserDetails.getUserId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "방 포스트 삭제 성공", null));
    }
}
