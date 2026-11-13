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

    // 참여 중인 방 모두 나가기 (회원탈퇴)
    public void leaveAllRooms(Long userId) {

        // 방 id 오름차순으로 잠가서 여러 방을 잠글 때 교착 상태를 막음
        for (Long roomId : roomUserRepository.findRoomIdsByUserId(userId)) {

            // 그 사이 방이 삭제됐거나 이미 나간 방은 건너뜀
            roomRepository.findByIdForUpdate(roomId)
                    .flatMap(room -> roomUserRepository.findByRoomIdAndUserIdForUpdate(roomId, userId))
                    .ifPresent(this::leaveRoom);
        }
    }

    // 방 나가기 (방장이면 가장 먼저 참여한 멤버에게 위임, 다른 멤버가 없으면 방 삭제)
    // 방 행을 잠그고 다시 조회한 멤버 정보로 호출해야 동시 요청에도 역할이 맞음
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
    // 모두 벌크 삭제: 회원탈퇴처럼 앞에서 조회한 트랜잭션에서도 최신 행을 지우고, 영속성 컨텍스트의 엔티티와 섞이지 않음
    public void deleteRoom(Long roomId) {
        roomPostRepository.deleteAllByRoomId(roomId);
        roomPlaylistRepository.deleteAllByRoomId(roomId);
        roomUserRepository.deleteAllByRoomId(roomId);
        roomRepository.deleteRoomById(roomId);
    }
}
