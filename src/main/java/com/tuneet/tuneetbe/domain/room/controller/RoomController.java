package com.tuneet.tuneetbe.domain.room.controller;

import com.tuneet.tuneetbe.domain.room.dto.request.RoomCreateRequest;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomCreateResponse;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomSummaryList;
import com.tuneet.tuneetbe.domain.room.service.RoomService;
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
}
