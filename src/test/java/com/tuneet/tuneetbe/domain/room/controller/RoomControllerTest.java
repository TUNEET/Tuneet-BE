package com.tuneet.tuneetbe.domain.room.controller;

import com.tuneet.tuneetbe.domain.room.dto.request.RoomEntryRequest;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomEntryResponse;
import com.tuneet.tuneetbe.domain.room.exception.RoomErrorCode;
import com.tuneet.tuneetbe.domain.room.service.RoomService;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 100L;

    @Mock
    private RoomService roomService;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private RoomController roomController;

    private RoomEntryRequest entry() {
        RoomEntryRequest request = BeanUtils.instantiateClass(RoomEntryRequest.class);
        ReflectionTestUtils.setField(request, "inviteCode", "aB3kP9xQ");
        return request;
    }

    private RoomEntryResponse response(boolean alreadyJoined) {
        return RoomEntryResponse.builder().roomId(ROOM_ID).alreadyJoined(alreadyJoined).build();
    }

    @Nested
    @DisplayName("방 입장")
    class EnterRoom {

        @Test
        @DisplayName("새로 입장하면 201과 본문 code 201을 반환한다")
        void created() {
            // given
            RoomEntryRequest request = entry();
            given(userDetails.getUserId()).willReturn(USER_ID);
            given(roomService.entryRoom(request, USER_ID)).willReturn(response(false));

            // when
            ResponseEntity<BaseResponse<RoomEntryResponse>> result = roomController.enterRoom(userDetails, request);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody().getCode()).isEqualTo(201);
            assertThat(result.getBody().getData().isAlreadyJoined()).isFalse();
        }

        @Test
        @DisplayName("이미 멤버면 200과 본문 code 200을 반환한다")
        void alreadyJoined() {
            // given
            RoomEntryRequest request = entry();
            given(userDetails.getUserId()).willReturn(USER_ID);
            given(roomService.entryRoom(request, USER_ID)).willReturn(response(true));

            // when
            ResponseEntity<BaseResponse<RoomEntryResponse>> result = roomController.enterRoom(userDetails, request);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getCode()).isEqualTo(200);
            assertThat(result.getBody().getData().isAlreadyJoined()).isTrue();
        }

        @Test
        @DisplayName("동시 요청으로 유니크 위반이 나면 한 번 다시 시도해 이미 멤버로 응답한다")
        void retriesOnDuplicate() {
            // given
            RoomEntryRequest request = entry();
            given(userDetails.getUserId()).willReturn(USER_ID);
            given(roomService.entryRoom(request, USER_ID))
                    .willThrow(new DataIntegrityViolationException("uk_room_users_room_user"))
                    .willReturn(response(true));

            // when
            ResponseEntity<BaseResponse<RoomEntryResponse>> result = roomController.enterRoom(userDetails, request);

            // then
            verify(roomService, times(2)).entryRoom(request, USER_ID);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getData().isAlreadyJoined()).isTrue();
        }

        @Test
        @DisplayName("다시 시도에서도 유니크 위반이 나면 예외를 그대로 던진다")
        void retryFailsAgain() {
            // given
            RoomEntryRequest request = entry();
            given(userDetails.getUserId()).willReturn(USER_ID);
            given(roomService.entryRoom(request, USER_ID))
                    .willThrow(new DataIntegrityViolationException("uk_room_users_room_user"));

            // when & then
            assertThatThrownBy(() -> roomController.enterRoom(userDetails, request))
                    .isInstanceOf(DataIntegrityViolationException.class);
            verify(roomService, times(2)).entryRoom(request, USER_ID);
        }

        @Test
        @DisplayName("비밀번호 오류 같은 비즈니스 예외는 다시 시도하지 않는다")
        void noRetryOnCustomException() {
            // given
            RoomEntryRequest request = entry();
            given(userDetails.getUserId()).willReturn(USER_ID);
            given(roomService.entryRoom(request, USER_ID))
                    .willThrow(new CustomException(RoomErrorCode.ROOM_PASSWORD_MISMATCH));

            // when & then
            assertThatThrownBy(() -> roomController.enterRoom(userDetails, request))
                    .isInstanceOf(CustomException.class);
            verify(roomService, times(1)).entryRoom(request, USER_ID);
        }
    }
}
