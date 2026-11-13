package com.tuneet.tuneetbe.domain.room.controller;

import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.exception.MusicErrorCode;
import com.tuneet.tuneetbe.domain.music.service.MusicCatalogService;
import com.tuneet.tuneetbe.domain.room.dto.request.RoomPlaylistCreateRequest;
import com.tuneet.tuneetbe.domain.room.dto.request.RoomPlaylistPosUpdateRequest;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomPlaylistListResponse;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomPlaylistResponse;
import com.tuneet.tuneetbe.domain.room.service.RoomPlaylistService;
import com.tuneet.tuneetbe.global.common.BaseResponse;
import com.tuneet.tuneetbe.global.exception.CustomException;
import com.tuneet.tuneetbe.global.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomPlaylistControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 100L;
    private static final Long PLAYLIST_ID = 30L;
    private static final String MUSIC_ID = "27e95178-49ac-47a1-8b4c-540c77982680";

    @Mock
    private RoomPlaylistService roomPlaylistService;

    @Mock
    private MusicCatalogService musicCatalogService;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private RoomPlaylistController roomPlaylistController;

    @Nested
    @DisplayName("방 플레이리스트 곡 추가")
    class CreateRoomPlaylist {

        @Test
        @DisplayName("곡을 먼저 확보한 뒤 서비스에 넘기고 201을 반환한다")
        void created() {
            // given
            Music music = BeanUtils.instantiateClass(Music.class);
            RoomPlaylistResponse response = RoomPlaylistResponse.builder().roomPlaylistId(PLAYLIST_ID).position(1).build();
            given(userDetails.getUserId()).willReturn(USER_ID);
            given(musicCatalogService.getOrCreateMusics(List.of(MUSIC_ID))).willReturn(List.of(music));
            given(roomPlaylistService.createRoomPlaylist(ROOM_ID, USER_ID, music)).willReturn(response);

            // when
            ResponseEntity<BaseResponse<RoomPlaylistResponse>> result =
                    roomPlaylistController.createRoomPlaylist(ROOM_ID, userDetails, createRequest(MUSIC_ID));

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody().getCode()).isEqualTo(201);
            assertThat(result.getBody().getData().getRoomPlaylistId()).isEqualTo(PLAYLIST_ID);
        }

        @Test
        @DisplayName("노래를 찾지 못하면 예외를 그대로 던지고 서비스를 호출하지 않는다")
        void musicNotFound() {
            // given
            given(musicCatalogService.getOrCreateMusics(List.of(MUSIC_ID)))
                    .willThrow(new CustomException(MusicErrorCode.MUSIC_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> roomPlaylistController.createRoomPlaylist(ROOM_ID, userDetails, createRequest(MUSIC_ID)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(MusicErrorCode.MUSIC_NOT_FOUND);

            verify(roomPlaylistService, never()).createRoomPlaylist(anyLong(), anyLong(), any());
        }
    }

    @Test
    @DisplayName("방 플레이리스트 전체 조회는 200과 목록을 반환한다")
    void getAllRoomPlaylist() {
        // given
        RoomPlaylistListResponse response = RoomPlaylistListResponse.builder().playlists(List.of()).build();
        given(userDetails.getUserId()).willReturn(USER_ID);
        given(roomPlaylistService.getAllRoomPlaylists(ROOM_ID, USER_ID)).willReturn(response);

        // when
        ResponseEntity<BaseResponse<RoomPlaylistListResponse>> result = roomPlaylistController.getAllRoomPlaylist(ROOM_ID, userDetails);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getCode()).isEqualTo(200);
        assertThat(result.getBody().getData().getPlaylists()).isEmpty();
    }

    @Test
    @DisplayName("방 플레이리스트 곡 삭제는 서비스를 호출하고 200을 반환한다")
    void deleteRoomPlaylist() {
        // given
        given(userDetails.getUserId()).willReturn(USER_ID);

        // when
        ResponseEntity<BaseResponse<Void>> result = roomPlaylistController.deleteRoomPlaylist(ROOM_ID, PLAYLIST_ID, userDetails);

        // then
        verify(roomPlaylistService).deleteRoomPlaylist(ROOM_ID, PLAYLIST_ID, USER_ID);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getCode()).isEqualTo(200);
        assertThat(result.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("방 플레이리스트 전체 삭제는 서비스를 호출하고 200을 반환한다")
    void deleteAllRoomPlaylist() {
        // given
        given(userDetails.getUserId()).willReturn(USER_ID);

        // when
        ResponseEntity<BaseResponse<Void>> result = roomPlaylistController.deleteAllRoomPlaylist(ROOM_ID, userDetails);

        // then
        verify(roomPlaylistService).deleteAllRoomPlaylists(ROOM_ID, USER_ID);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getCode()).isEqualTo(200);
        assertThat(result.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("방 플레이리스트 순서 변경은 200과 바뀐 목록을 반환한다")
    void updateRoomPlaylistPos() {
        // given
        RoomPlaylistPosUpdateRequest request = BeanUtils.instantiateClass(RoomPlaylistPosUpdateRequest.class);
        ReflectionTestUtils.setField(request, "playListIdList", List.of(32L, 31L));
        RoomPlaylistListResponse response = RoomPlaylistListResponse.builder()
                .playlists(List.of(
                        RoomPlaylistResponse.builder().roomPlaylistId(32L).position(1).build(),
                        RoomPlaylistResponse.builder().roomPlaylistId(31L).position(2).build()))
                .build();
        given(userDetails.getUserId()).willReturn(USER_ID);
        given(roomPlaylistService.updateRoomPlaylistPos(ROOM_ID, USER_ID, request)).willReturn(response);

        // when
        ResponseEntity<BaseResponse<RoomPlaylistListResponse>> result =
                roomPlaylistController.updateRoomPlaylistPos(ROOM_ID, userDetails, request);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getCode()).isEqualTo(200);
        assertThat(result.getBody().getData().getPlaylists())
                .extracting(RoomPlaylistResponse::getRoomPlaylistId)
                .containsExactly(32L, 31L);
    }

    private RoomPlaylistCreateRequest createRequest(String musicId) {
        RoomPlaylistCreateRequest request = BeanUtils.instantiateClass(RoomPlaylistCreateRequest.class);
        ReflectionTestUtils.setField(request, "musicId", musicId);
        return request;
    }
}
