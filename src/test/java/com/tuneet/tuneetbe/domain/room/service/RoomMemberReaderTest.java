package com.tuneet.tuneetbe.domain.room.service;

import com.tuneet.tuneetbe.domain.room.entity.Room;
import com.tuneet.tuneetbe.domain.room.entity.RoomUser;
import com.tuneet.tuneetbe.domain.room.enums.RoomRole;
import com.tuneet.tuneetbe.domain.room.exception.RoomErrorCode;
import com.tuneet.tuneetbe.domain.room.repository.RoomRepository;
import com.tuneet.tuneetbe.domain.room.repository.RoomUserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomMemberReaderTest {

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 100L;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomUserRepository roomUserRepository;

    @InjectMocks
    private RoomMemberReader roomMemberReader;

    @Test
    @DisplayName("방 멤버면 내 멤버 정보를 역할과 함께 반환한다")
    void getMember() {
        // given
        Room room = room();
        RoomUser roomUser = RoomUser.builder().room(room).role(RoomRole.OWNER).build();
        given(roomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(roomUserRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(roomUser));

        // when
        RoomUser result = roomMemberReader.getMember(ROOM_ID, USER_ID);

        // then
        assertThat(result).isSameAs(roomUser);
        assertThat(result.getRole()).isEqualTo(RoomRole.OWNER);
    }

    @Test
    @DisplayName("방 멤버면 방을 반환한다")
    void getRoomAsMember() {
        // given
        Room room = room();
        given(roomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(roomUserRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID))
                .willReturn(Optional.of(RoomUser.builder().room(room).role(RoomRole.MEMBER).build()));

        // when
        Room result = roomMemberReader.getRoomAsMember(ROOM_ID, USER_ID);

        // then
        assertThat(result).isSameAs(room);
    }

    @Test
    @DisplayName("방이 없으면 ROOM_NOT_FOUND 예외가 발생하고 멤버를 조회하지 않는다")
    void roomNotFound() {
        // given
        given(roomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomMemberReader.getMember(ROOM_ID, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(RoomErrorCode.ROOM_NOT_FOUND);

        verify(roomUserRepository, never()).findByRoomIdAndUserId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("방 멤버가 아니면 NOT_ROOM_MEMBER 예외가 발생한다")
    void notMember() {
        // given
        given(roomRepository.findById(ROOM_ID)).willReturn(Optional.of(room()));
        given(roomUserRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomMemberReader.getMember(ROOM_ID, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(RoomErrorCode.NOT_ROOM_MEMBER);
    }

    @Test
    @DisplayName("멤버 구성을 바꿀 때는 방을 잠근 뒤 잠금 조회로 내 멤버 정보를 반환한다")
    void getMemberForUpdate() {
        // given
        Room room = room();
        RoomUser roomUser = RoomUser.builder().room(room).role(RoomRole.OWNER).build();
        given(roomRepository.findByIdForUpdate(ROOM_ID)).willReturn(Optional.of(room));
        given(roomUserRepository.findByRoomIdAndUserIdForUpdate(ROOM_ID, USER_ID)).willReturn(Optional.of(roomUser));

        // when
        RoomUser result = roomMemberReader.getMemberForUpdate(ROOM_ID, USER_ID);

        // then
        assertThat(result).isSameAs(roomUser);
        InOrder inOrder = inOrder(roomRepository, roomUserRepository);
        inOrder.verify(roomRepository).findByIdForUpdate(ROOM_ID);
        inOrder.verify(roomUserRepository).findByRoomIdAndUserIdForUpdate(ROOM_ID, USER_ID);
        verify(roomRepository, never()).findById(anyLong());
        verify(roomUserRepository, never()).findByRoomIdAndUserId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("잠글 방이 없으면 ROOM_NOT_FOUND 예외가 발생하고 멤버를 조회하지 않는다")
    void getMemberForUpdateRoomNotFound() {
        // given
        given(roomRepository.findByIdForUpdate(ROOM_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomMemberReader.getMemberForUpdate(ROOM_ID, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(RoomErrorCode.ROOM_NOT_FOUND);

        verify(roomUserRepository, never()).findByRoomIdAndUserIdForUpdate(anyLong(), anyLong());
    }

    @Test
    @DisplayName("잠근 방의 멤버가 아니면 NOT_ROOM_MEMBER 예외가 발생한다")
    void getMemberForUpdateNotMember() {
        // given
        given(roomRepository.findByIdForUpdate(ROOM_ID)).willReturn(Optional.of(room()));
        given(roomUserRepository.findByRoomIdAndUserIdForUpdate(ROOM_ID, USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomMemberReader.getMemberForUpdate(ROOM_ID, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(RoomErrorCode.NOT_ROOM_MEMBER);
    }

    private Room room() {
        Room room = Room.builder()
                .roomTitle("퇴근길 플리 모임")
                .roomDescription("퇴근길에 듣는 노래를 공유해요")
                .inviteCode("aB3kP9xQ")
                .build();
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        return room;
    }
}
