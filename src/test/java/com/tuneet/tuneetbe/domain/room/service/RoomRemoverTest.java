package com.tuneet.tuneetbe.domain.room.service;

import com.tuneet.tuneetbe.domain.room.entity.Room;
import com.tuneet.tuneetbe.domain.room.entity.RoomUser;
import com.tuneet.tuneetbe.domain.room.enums.RoomRole;
import com.tuneet.tuneetbe.domain.room.repository.RoomPlaylistRepository;
import com.tuneet.tuneetbe.domain.room.repository.RoomPostRepository;
import com.tuneet.tuneetbe.domain.room.repository.RoomRepository;
import com.tuneet.tuneetbe.domain.room.repository.RoomUserRepository;
import com.tuneet.tuneetbe.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomRemoverTest {

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 100L;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomUserRepository roomUserRepository;

    @Mock
    private RoomPostRepository roomPostRepository;

    @Mock
    private RoomPlaylistRepository roomPlaylistRepository;

    @InjectMocks
    private RoomRemover roomRemover;

    @Test
    @DisplayName("멤버가 나가면 위임 없이 내 멤버 정보만 삭제한다")
    void leaveAsMember() {
        // given
        RoomUser me = roomUser(USER_ID, RoomRole.MEMBER);

        // when
        roomRemover.leaveRoom(me);

        // then
        verify(roomUserRepository, never()).findFirstByRoomIdAndUserIdNotOrderByIdAsc(anyLong(), anyLong());
        verify(roomUserRepository).delete(me);
        verify(roomRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("방장이 나가면 가장 먼저 참여한 멤버에게 위임하고 방을 유지한다")
    void leaveAsOwnerWithMembers() {
        // given
        RoomUser me = roomUser(USER_ID, RoomRole.OWNER);
        RoomUser nextOwner = roomUser(2L, RoomRole.MEMBER);
        given(roomUserRepository.findFirstByRoomIdAndUserIdNotOrderByIdAsc(ROOM_ID, USER_ID))
                .willReturn(Optional.of(nextOwner));

        // when
        roomRemover.leaveRoom(me);

        // then
        assertThat(nextOwner.getRole()).isEqualTo(RoomRole.OWNER);
        verify(roomUserRepository).delete(me);
        verify(roomRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("방장이 나가고 다른 멤버가 없으면 방을 삭제한다")
    void leaveAsLastOwner() {
        // given
        RoomUser me = roomUser(USER_ID, RoomRole.OWNER);
        given(roomUserRepository.findFirstByRoomIdAndUserIdNotOrderByIdAsc(ROOM_ID, USER_ID))
                .willReturn(Optional.empty());

        // when
        roomRemover.leaveRoom(me);

        // then
        verify(roomRepository).deleteById(ROOM_ID);
        verify(roomUserRepository, never()).delete(any());
    }

    @Test
    @DisplayName("방을 삭제하면 포스트, 플레이리스트, 멤버, 방 순서로 삭제한다")
    void deleteRoom() {
        // when
        roomRemover.deleteRoom(ROOM_ID);

        // then
        InOrder inOrder = inOrder(roomPostRepository, roomPlaylistRepository, roomUserRepository, roomRepository);
        inOrder.verify(roomPostRepository).deleteAllByRoomId(ROOM_ID);
        inOrder.verify(roomPlaylistRepository).deleteAllByRoomId(ROOM_ID);
        inOrder.verify(roomUserRepository).deleteAllByRoomId(ROOM_ID);
        inOrder.verify(roomRepository).deleteById(ROOM_ID);
    }

    private RoomUser roomUser(Long userId, RoomRole role) {
        Room room = BeanUtils.instantiateClass(Room.class);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", userId);
        return RoomUser.builder().room(room).user(user).role(role).build();
    }
}
