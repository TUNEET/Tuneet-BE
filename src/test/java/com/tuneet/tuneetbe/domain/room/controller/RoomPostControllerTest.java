package com.tuneet.tuneetbe.domain.room.controller;

import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.exception.MusicErrorCode;
import com.tuneet.tuneetbe.domain.music.service.MusicCatalogService;
import com.tuneet.tuneetbe.domain.room.dto.request.RoomPostCreateRequest;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomPostListResponse;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomPostResponse;
import com.tuneet.tuneetbe.domain.room.service.RoomPostService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomPostControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 100L;
    private static final Long POST_ID = 20L;
    private static final String MUSIC_ID = "27e95178-49ac-47a1-8b4c-540c77982680";

    @Mock
    private RoomPostService roomPostService;

    @Mock
    private MusicCatalogService musicCatalogService;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private RoomPostController roomPostController;

    @Nested
    @DisplayName("방 포스트 작성")
    class CreateRoomPost {

        @Test
        @DisplayName("곡을 먼저 확보한 뒤 서비스에 넘기고 201을 반환한다")
        void created() {
            // given
            RoomPostCreateRequest request = new RoomPostCreateRequest(MUSIC_ID, "요즘 계속 듣는 노래");
            Music music = BeanUtils.instantiateClass(Music.class);
            RoomPostResponse response = RoomPostResponse.builder().roomPostId(POST_ID).isMine(true).build();
            given(userDetails.getUserId()).willReturn(USER_ID);
            given(musicCatalogService.getOrCreateMusics(List.of(MUSIC_ID))).willReturn(List.of(music));
            given(roomPostService.createRoomPost(ROOM_ID, USER_ID, music, "요즘 계속 듣는 노래")).willReturn(response);

            // when
            ResponseEntity<BaseResponse<RoomPostResponse>> result = roomPostController.createRoomPost(userDetails, request, ROOM_ID);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody().getCode()).isEqualTo(201);
            assertThat(result.getBody().getData().getRoomPostId()).isEqualTo(POST_ID);
        }

        @Test
        @DisplayName("노래를 찾지 못하면 예외를 그대로 던지고 서비스를 호출하지 않는다")
        void musicNotFound() {
            // given
            RoomPostCreateRequest request = new RoomPostCreateRequest(MUSIC_ID, "코멘트");
            given(musicCatalogService.getOrCreateMusics(List.of(MUSIC_ID)))
                    .willThrow(new CustomException(MusicErrorCode.MUSIC_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> roomPostController.createRoomPost(userDetails, request, ROOM_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(MusicErrorCode.MUSIC_NOT_FOUND);

            verify(roomPostService, never()).createRoomPost(anyLong(), anyLong(), any(), anyString());
        }
    }

    @Test
    @DisplayName("방 포스트 목록 조회는 200과 목록을 반환한다")
    void getRoomPostList() {
        // given
        RoomPostListResponse response = RoomPostListResponse.builder().roomPosts(List.of()).hasNext(false).build();
        given(userDetails.getUserId()).willReturn(USER_ID);
        given(roomPostService.getRoomPostList(ROOM_ID, USER_ID, 21L, 20)).willReturn(response);

        // when
        ResponseEntity<BaseResponse<RoomPostListResponse>> result = roomPostController.getRoomPostList(userDetails, ROOM_ID, 21L, 20);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getCode()).isEqualTo(200);
        assertThat(result.getBody().getData().getHasNext()).isFalse();
    }

    @Test
    @DisplayName("방 포스트 삭제는 서비스를 호출하고 200을 반환한다")
    void deleteRoomPost() {
        // given
        given(userDetails.getUserId()).willReturn(USER_ID);

        // when
        ResponseEntity<BaseResponse<Void>> result = roomPostController.deleteRoomPost(userDetails, ROOM_ID, POST_ID);

        // then
        verify(roomPostService).deleteRoomPost(ROOM_ID, POST_ID, USER_ID);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getCode()).isEqualTo(200);
        assertThat(result.getBody().getData()).isNull();
    }
}
