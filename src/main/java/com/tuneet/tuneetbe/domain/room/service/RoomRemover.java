package com.tuneet.tuneetbe.domain.room.service;

import com.tuneet.tuneetbe.domain.room.entity.RoomUser;
import com.tuneet.tuneetbe.domain.room.enums.RoomRole;
import com.tuneet.tuneetbe.domain.room.repository.RoomPlaylistRepository;
import com.tuneet.tuneetbe.domain.room.repository.RoomPostRepository;
import com.tuneet.tuneetbe.domain.room.repository.RoomRepository;
import com.tuneet.tuneetbe.domain.room.repository.RoomUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

// 방 나가기, 방 삭제 (회원탈퇴에서도 사용)
@Component
@RequiredArgsConstructor
public class RoomRemover {

    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final RoomPostRepository roomPostRepository;
    private final RoomPlaylistRepository roomPlaylistRepository;

    // 방 나가기 (방장이면 가장 먼저 참여한 멤버에게 위임, 다른 멤버가 없으면 방 삭제)
    public void leaveRoom(RoomUser roomUser) {

        Long roomId = roomUser.getRoom().getId();
        Long userId = roomUser.getUser().getId();

        if (roomUser.getRole() == RoomRole.OWNER) {

            // 1. 가장 먼저 참여한 멤버 조회
            Optional<RoomUser> nextOwner = roomUserRepository.findFirstByRoomIdAndUserIdNotOrderByIdAsc(roomId, userId);

            // 2. 다른 멤버가 없으면 방 삭제
            if (nextOwner.isEmpty()) {
                deleteRoom(roomId);
                return;
            }

            // 3. 다른 멤버가 있으면 방장 위임
            nextOwner.get().delegateOwner();
        }

        roomUserRepository.delete(roomUser);
    }

    // 방 삭제 (방의 자식 데이터부터 삭제)
    public void deleteRoom(Long roomId) {
        roomPostRepository.deleteAllByRoomId(roomId);
        roomPlaylistRepository.deleteAllByRoomId(roomId);
        roomUserRepository.deleteAllByRoomId(roomId);
        roomRepository.deleteById(roomId);
    }
}
