package com.tuneet.tuneetbe.domain.room.controller;

import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.service.MusicCatalogService;
import com.tuneet.tuneetbe.domain.room.dto.request.RoomPlaylistCreateRequest;
import com.tuneet.tuneetbe.domain.room.dto.request.RoomPlaylistPosUpdateRequest;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomPlaylistListResponse;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomPlaylistResponse;
import com.tuneet.tuneetbe.domain.room.service.RoomPlaylistService;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
@Tag(name = "Room", description = "방 API")
public class RoomPlaylistController {

    private final MusicCatalogService musicCatalogService;
    private final RoomPlaylistService roomPlaylistService;

    @Operation(summary = "방 플레이리스트 곡 추가", description = "방 플레이리스트 맨 뒤에 노래를 추가하는 API. 같은 방에 같은 곡은 추가할 수 없음. 오류: G001 입력값 오류, M4001 존재하지 않는 노래, M5001 음악 서버 통신 실패, U4002 존재하지 않는 사용자, R4001 존재하지 않는 방, R4004 방 멤버 아님, R4007 이미 플레이리스트에 있는 곡")
    @PostMapping("/{room-id}/playlists")
    public ResponseEntity<BaseResponse<RoomPlaylistResponse>> createRoomPlaylist(@PathVariable("room-id") Long roomId,
                                                                                 @AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                                 @Valid @RequestBody RoomPlaylistCreateRequest request){
        Music music = musicCatalogService.getOrCreateMusics(List.of(request.getMusicId())).getFirst();

        RoomPlaylistResponse response = roomPlaylistService.createRoomPlaylist(roomId, customUserDetails.getUserId(), music);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(201, "방 플레이리스트 생성 성공",  response));

    }

    @Operation(summary = "방 플레이리스트 전체 조회", description = "방 플레이리스트를 재생 순서(position)대로 전체 조회하는 API. 페이지 없이 모두 반환. 오류: R4001 존재하지 않는 방, R4004 방 멤버 아님")
    @GetMapping("/{room-id}/playlists")
    public ResponseEntity<BaseResponse<RoomPlaylistListResponse>> getAllRoomPlaylist(@PathVariable("room-id") Long roomId,
                                                                                     @AuthenticationPrincipal CustomUserDetails customUserDetails){

        RoomPlaylistListResponse response = roomPlaylistService.getAllRoomPlaylists(roomId, customUserDetails.getUserId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "방 플레이리스트 전체 조회 성공", response));
    }

    @Operation(summary = "방 플레이리스트 곡 삭제", description = "플레이리스트에서 곡 하나를 삭제하는 API. 곡을 추가한 사람과 방장만 삭제 가능. playlist-id는 roomPlaylistId. 오류: R4001 존재하지 않는 방, R4004 방 멤버 아님, R4008 곡이 없거나 다른 방의 곡, R4009 추가한 사람도 방장도 아님")
    @DeleteMapping("/{room-id}/playlists/{playlist-id}")
    public ResponseEntity<BaseResponse<Void>> deleteRoomPlaylist(@PathVariable("room-id") Long roomId,
                                                                 @PathVariable("playlist-id") Long playlistId,
                                                                 @AuthenticationPrincipal CustomUserDetails customUserDetails){

        roomPlaylistService.deleteRoomPlaylist(roomId, playlistId, customUserDetails.getUserId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "방 플레이리스트 삭제 성공", null));
    }

    @Operation(summary = "방 플레이리스트 전체 삭제", description = "방 플레이리스트의 곡을 모두 삭제하는 API. 방장만 가능. 오류: R4001 존재하지 않는 방, R4004 방 멤버 아님, R4010 방장 아님")
    @DeleteMapping("/{room-id}/playlists")
    public ResponseEntity<BaseResponse<Void>> deleteAllRoomPlaylist(@PathVariable("room-id") Long roomId,
                                                                    @AuthenticationPrincipal CustomUserDetails customUserDetails){

        roomPlaylistService.deleteAllRoomPlaylists(roomId, customUserDetails.getUserId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "방 플레이리스트 전체 삭제 성공", null));
    }

    @Operation(summary = "방 플레이리스트 순서 변경", description = "현재 플레이리스트의 roomPlaylistId 전체를 원하는 순서대로 보내면 position을 1부터 다시 매기고 바뀐 목록을 반환하는 API. 그사이 곡이 추가·삭제되어 목록이 다르면 새로 조회 후 다시 요청. 오류: G001 입력값 오류, R4001 존재하지 않는 방, R4004 방 멤버 아님, R4011 보낸 목록이 현재 플레이리스트와 다름")
    @PutMapping("/{room-id}/playlists/order")
    public ResponseEntity<BaseResponse<RoomPlaylistListResponse>> updateRoomPlaylistPos(@PathVariable("room-id") Long roomId,
                                                                                        @AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                                        @Valid @RequestBody RoomPlaylistPosUpdateRequest request){

        RoomPlaylistListResponse response = roomPlaylistService.updateRoomPlaylistPos(roomId, customUserDetails.getUserId(), request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "플레이리스트 순서 변경 성공", response));
    }
}
