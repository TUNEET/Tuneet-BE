package com.tuneet.tuneetbe.domain.room.service;

import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.room.dto.request.RoomPlaylistPosUpdateRequest;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomPlaylistListResponse;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomPlaylistResponse;
import com.tuneet.tuneetbe.domain.room.entity.Room;
import com.tuneet.tuneetbe.domain.room.entity.RoomPlaylist;
import com.tuneet.tuneetbe.domain.room.entity.RoomUser;
import com.tuneet.tuneetbe.domain.room.enums.RoomRole;
import com.tuneet.tuneetbe.domain.room.exception.RoomErrorCode;
import com.tuneet.tuneetbe.domain.room.repository.RoomPlaylistRepository;
import com.tuneet.tuneetbe.domain.room.repository.RoomRepository;
import com.tuneet.tuneetbe.domain.room.repository.RoomUserRepository;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoomPlaylistService {

    private final RoomPlaylistRepository roomPlaylistRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;

    @Transactional
    public RoomPlaylistResponse createRoomPlaylist(Long roomId, Long userId, Music music) {

        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        Room room = getRoomAsMember(roomId, userId);

        if (roomPlaylistRepository.existsByRoomAndMusic(room, music)) {
            throw new CustomException(RoomErrorCode.ROOM_PLAYLIST_DUPLICATE_MUSIC);
        }

        Integer pos = roomPlaylistRepository.findMaxPosition(roomId) + 1;

        RoomPlaylist roomPlaylist = RoomPlaylist.builder()
                .room(room)
                .music(music)
                .user(user)
                .position(pos)
                .build();

        RoomPlaylist savedRoomPlaylist;
        try {
            savedRoomPlaylist = roomPlaylistRepository.saveAndFlush(roomPlaylist);
        } catch (DataIntegrityViolationException e) {
            // 같은 곡이 동시에 추가된 경우
            throw new CustomException(RoomErrorCode.ROOM_PLAYLIST_DUPLICATE_MUSIC);
        }

        return RoomPlaylistResponse.from(savedRoomPlaylist);
    }

    public RoomPlaylistListResponse getAllRoomPlaylists(Long roomId, Long userId) {

        Room room = getRoomAsMember(roomId, userId);

        List<RoomPlaylist> roomPlaylistList = roomPlaylistRepository.findByRoomOrderByPositionAscIdAsc(room);

        return RoomPlaylistListResponse.from(roomPlaylistList);
    }

    @Transactional
    public void deleteRoomPlaylist(Long roomId, Long roomPlaylistId, Long userId) {

        // 1. 방 멤버 검사 + 내 역할(Role) 탐색
        RoomUser me = getRoomMember(roomId, userId);

        // 2. 플레이리스트 원소 가져오기(만약 다른 방의 플레이리스트이거나, 아예 존재하지 않는다면 동일하게 NOTFOUND)
        RoomPlaylist roomPlaylist = roomPlaylistRepository.findByRoomAndId(me.getRoom(), roomPlaylistId)
                .orElseThrow(() -> new CustomException(RoomErrorCode.ROOM_PLAYLIST_NOT_FOUND));

        // 주인이 아니고, 똑같지도 않다면 -> 삭제 불가
        if (me.getRole() != RoomRole.OWNER && !roomPlaylist.getUser().getId().equals(userId)) {
            throw new CustomException(RoomErrorCode.ROOM_PLAYLIST_ACCESS_DENIED);
        }

        roomPlaylistRepository.delete(roomPlaylist);
    }

    @Transactional
    public void deleteAllRoomPlaylists(Long roomId, Long userId) {

        // 1. 방 멤버 검사 + 내 역할 탐색
        RoomUser me = getRoomMember(roomId, userId);

        // 2. 주인이 아니라면 -> 삭제 불가
        if (me.getRole() != RoomRole.OWNER) {
            throw new CustomException(RoomErrorCode.ROOM_OWNER_ONLY);
        }

        // 3. 삭제
        roomPlaylistRepository.deleteAllByRoomId(roomId);
    }

    @Transactional
    public RoomPlaylistListResponse updateRoomPlaylistPos(Long roomId, Long userId, RoomPlaylistPosUpdateRequest request) {

        // 1. 방 멤버 검사
        Room room = getRoomAsMember(roomId, userId);

        // 2. 플레이리스트 원소 모두 가져오기
        // 업데이트가 항상 같은 순서로 나가도록 id 순으로 조회 (동시 순서 변경 데드락 방지)
        List<RoomPlaylist> roomPlaylistList = roomPlaylistRepository.findByRoomOrderByIdAsc(room);

        List<Long> ids = request.getPlayListIdList();
        Map<Long, RoomPlaylist> byId = roomPlaylistList.stream()
                .collect(Collectors.toMap(RoomPlaylist::getId, rp -> rp));

        // 개수가 다르거나, 중복 id가 있거나, 이 방에 없는 id가 있으면 지금 목록과 다른 것
        if (ids.size() != byId.size() || new HashSet<>(ids).size() != ids.size() || !byId.keySet().containsAll(ids)) {
            throw new CustomException(RoomErrorCode.ROOM_PLAYLIST_ORDER_MISMATCH);
        }

        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).changePosition(i + 1);
        }

        roomPlaylistList.sort(Comparator.comparing(RoomPlaylist::getPosition));
        return RoomPlaylistListResponse.from(roomPlaylistList);
    }

    // 방이 있고 내가 멤버인지 확인
    private Room getRoomAsMember(Long roomId, Long userId) {
        return getRoomMember(roomId, userId).getRoom();
    }

    // 방이 있고 내가 멤버인지 확인 후 내 멤버 정보(역할 포함) 반환
    private RoomUser getRoomMember(Long roomId, Long userId) {
        if (!roomRepository.existsById(roomId)) {
            throw new CustomException(RoomErrorCode.ROOM_NOT_FOUND);
        }
        return roomUserRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(RoomErrorCode.NOT_ROOM_MEMBER));
    }
}
