package com.tuneet.tuneetbe.domain.room.service;

import com.tuneet.tuneetbe.domain.room.entity.Room;
import com.tuneet.tuneetbe.domain.room.entity.RoomUser;
import com.tuneet.tuneetbe.domain.room.exception.RoomErrorCode;
import com.tuneet.tuneetbe.domain.room.repository.RoomRepository;
import com.tuneet.tuneetbe.domain.room.repository.RoomUserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 방 하위 기능(포스트, 플레이리스트 등)에서 쓰는 방 멤버 확인
@Component
@RequiredArgsConstructor
public class RoomMemberReader {

    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;

    // 방이 있고 내가 멤버인지 확인
    public Room getRoomAsMember(Long roomId, Long userId) {
        return getMember(roomId, userId).getRoom();
    }

    // 방이 있고 내가 멤버인지 확인 후 내 멤버 정보(역할 포함) 반환
    public RoomUser getMember(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(RoomErrorCode.ROOM_NOT_FOUND));
        return roomUserRepository.findByRoomIdAndUserId(room.getId(), userId)
                .orElseThrow(() -> new CustomException(RoomErrorCode.NOT_ROOM_MEMBER));
    }

    // 방 행을 잠그고 내 멤버 정보 반환 (방 나가기, 삭제처럼 멤버 구성이 바뀔 때 사용)
    public RoomUser getMemberForUpdate(Long roomId, Long userId) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(RoomErrorCode.ROOM_NOT_FOUND));
        return roomUserRepository.findByRoomIdAndUserIdForUpdate(room.getId(), userId)
                .orElseThrow(() -> new CustomException(RoomErrorCode.NOT_ROOM_MEMBER));
    }
}
