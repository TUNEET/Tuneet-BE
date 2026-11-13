package com.tuneet.tuneetbe.domain.room.controller;

import com.tuneet.tuneetbe.domain.room.dto.request.RoomCreateRequest;
import com.tuneet.tuneetbe.domain.room.dto.request.RoomEntryRequest;
import com.tuneet.tuneetbe.domain.room.dto.response.*;
import com.tuneet.tuneetbe.domain.room.service.RoomService;
import com.tuneet.tuneetbe.global.common.BaseResponse;
import com.tuneet.tuneetbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
@Tag(name = "Room", description = "방 API")
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "내 방 목록 조회", description = "로그인한 사용자가 참여 중인 방 목록을 최근 참여순으로 조회하는 API")
    @GetMapping
    public ResponseEntity<BaseResponse<RoomSummaryList>> getRooms(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

        RoomSummaryList roomSummaryList = roomService.getRoomSummaryList(customUserDetails.getUserId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "방 리스트 조회 성공", roomSummaryList));
    }

    @Operation(summary = "방 생성", description = "방을 만들고 초대 코드를 받는 API. 만든 사용자는 방장(OWNER)으로 참여한다")
    @PostMapping
    public ResponseEntity<BaseResponse<RoomCreateResponse>> createRoom(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                       @Valid @RequestBody RoomCreateRequest roomCreateRequest) {

        RoomCreateResponse roomCreateResponse = roomService.createRoom(roomCreateRequest, customUserDetails.getUserId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(201, "방 생성 성공", roomCreateResponse));
    }

    @Operation(summary = "방 입장", description = "초대 코드(링크·QR)로 방에 입장하는 API. 잠긴 방은 비밀번호 4자리가 필요하며, 비밀번호 없이 요청하면 R4002를 응답한다")
    @PostMapping("/entry")
    public ResponseEntity<BaseResponse<RoomEntryResponse>> enterRoom(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                     @Valid @RequestBody RoomEntryRequest roomEntryRequest) {
        RoomEntryResponse roomEntryResponse;
        try {
            roomEntryResponse = roomService.entryRoom(roomEntryRequest, customUserDetails.getUserId());
        } catch (DataIntegrityViolationException e) {
            // 같은 사용자의 요청이 동시에 와서 먼저 온 요청이 이미 입장시킨 경우 -> 다시 조회하면 alreadyJoined
            roomEntryResponse = roomService.entryRoom(roomEntryRequest, customUserDetails.getUserId());
        }
        boolean joined = roomEntryResponse.isAlreadyJoined();
        return ResponseEntity
                .status(joined ? HttpStatus.OK : HttpStatus.CREATED)
                .body(BaseResponse.success(joined ? 200 : 201, joined ? "이미 참여 중인 방" : "방 입장 성공", roomEntryResponse));
    }

    @Operation(summary = "방 홈 화면 조회", description = "방 정보, 내 역할, 멤버 목록, 최근 포스트 2개, 플레이리스트 앞 2곡을 한 번에 조회하는 API. 방 멤버만 조회할 수 있으며, 멤버가 아니면 R4004를 응답한다")
    @GetMapping("/{room-id}")
    public ResponseEntity<BaseResponse<RoomHomeResponse>> getRoomHome(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                         @PathVariable("room-id") Long roomId) {
        RoomHomeResponse roomHomeResponse = roomService.getRoomHome(roomId, customUserDetails.getUserId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "방 홈 화면 조회 성공", roomHomeResponse));
    }

    @Operation(summary = "방 멤버 목록 조회", description = "방 정보와 나를 제외한 멤버 목록을 조회하는 API. 방 멤버만 조회할 수 있으며, 멤버가 아니면 R4004를 응답한다")
    @GetMapping("/{room-id}/users")
    public ResponseEntity<BaseResponse<RoomUserListResponse>> getRoomUsers(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                           @PathVariable("room-id") Long roomId) {
        RoomUserListResponse roomUserListResponse = roomService.getRoomUserList(roomId, customUserDetails.getUserId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "방 유저 목록 조회 성공", roomUserListResponse));
    }

    @Operation(summary = "방 나가기", description = "방에서 나가는 API. 방장이 나가면 가장 먼저 참여한 멤버에게 방장을 넘기고, 다른 멤버가 없으면 방을 삭제한다. 내가 올린 포스트와 플레이리스트 곡은 남는다. 오류: R4001 존재하지 않는 방, R4004 방 멤버 아님")
    @DeleteMapping("/{room-id}/users/me")
    public ResponseEntity<BaseResponse<Void>> leaveRoom(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                        @PathVariable("room-id") Long roomId) {

        roomService.leaveRoom(roomId, customUserDetails.getUserId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "방 나가기 성공", null));
    }

    @Operation(summary = "방 삭제", description = "방과 방의 포스트, 플레이리스트, 멤버 정보를 모두 삭제하는 API. 방장만 가능. 오류: R4001 존재하지 않는 방, R4004 방 멤버 아님, R4010 방장 아님")
    @DeleteMapping("/{room-id}")
    public ResponseEntity<BaseResponse<Void>> deleteRoom(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                         @PathVariable("room-id") Long roomId) {

        roomService.deleteRoom(roomId, customUserDetails.getUserId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "방 삭제 성공", null));
    }
}
