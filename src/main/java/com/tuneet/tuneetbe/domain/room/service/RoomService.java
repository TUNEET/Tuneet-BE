package com.tuneet.tuneetbe.domain.room.service;

import com.tuneet.tuneetbe.domain.room.dto.request.RoomCreateRequest;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomCreateResponse;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomSummary;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomSummaryList;
import com.tuneet.tuneetbe.domain.room.entity.Room;
import com.tuneet.tuneetbe.domain.room.entity.RoomUser;
import com.tuneet.tuneetbe.domain.room.enums.RoomRole;
import com.tuneet.tuneetbe.domain.room.exception.RoomErrorCode;
import com.tuneet.tuneetbe.domain.room.repository.RoomRepository;
import com.tuneet.tuneetbe.domain.room.repository.RoomUserRepository;
import com.tuneet.tuneetbe.domain.room.util.InviteCodeGenerator;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;
    private final InviteCodeGenerator inviteCodeGenerator;

    // 방 생성
    @Transactional
    public RoomCreateResponse createRoom(RoomCreateRequest request, Long userId) {

        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 비밀번호 존재 시, 암호화
        String encodedPassword = (request.getPassword() == null) ? null
                : passwordEncoder.encode(request.getPassword());

        String inviteCode = generateInviteCode();

        Room room = Room.builder()
                .roomTitle(request.getRoomTitle())
                .roomPicture(request.getRoomPicture())
                .password(encodedPassword)
                .roomDescription(request.getRoomDescription())
                .inviteCode(inviteCode)
                .build();

        Room savedRoom = roomRepository.save(room);

        RoomUser roomUser = RoomUser.builder()
                .room(savedRoom)
                .user(user)
                .role(RoomRole.OWNER)
                .build();

        roomUserRepository.save(roomUser);

        log.info("[RoomService] 방 생성 성공: roomId = {}, userId = {}", savedRoom.getId(), userId);

        return generateRoomCreateResponse(savedRoom);
    }

    public RoomSummaryList getRoomSummaryList(Long userId) {

        // 1. 유저 존재 여부 검증
        if (!userRepository.existsById(userId)) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }

        // 2. 유저 소속 룸 유저 목록 조회
        List<RoomUser> roomUserList = roomUserRepository.findAllByUserIdOrderByIdDesc(userId);

        List<Room> roomList = new ArrayList<>();

        for (RoomUser roomUser : roomUserList) {
            roomList.add(roomUser.getRoom());
        }

        RoomSummaryList roomSummaryList = generateRoomSummaryList(roomList);

        return roomSummaryList;

    }

    // 초대코드 생성
    private String generateInviteCode() {
        for (int i = 0; i < 5; i++) {
            String inviteCode = inviteCodeGenerator.generate(8);
            if (!roomRepository.existsByInviteCode(inviteCode)) {
                return inviteCode;
            }
        }
        throw new CustomException(RoomErrorCode.INVITE_CODE_GENERATION_FAILED);
    }

    // RoomCreateResponse 조립기
    private RoomCreateResponse generateRoomCreateResponse(Room room) {
        return RoomCreateResponse.builder()
                .roomId(room.getId())
                .roomPicture(room.getRoomPicture())
                .roomTitle(room.getRoomTitle())
                .roomDescription(room.getRoomDescription())
                .inviteCode(room.getInviteCode())
                .hasPassword(room.getPassword() != null)
                .build();
    }

    private RoomSummaryList generateRoomSummaryList(List<Room> roomList) {

        RoomSummaryList roomSummaryList = new RoomSummaryList();

        for (Room room : roomList) {
            roomSummaryList.addRoomSummaryList(generateRoomSummary(room));
        }

        return roomSummaryList;
    }

    private RoomSummary generateRoomSummary(Room room) {
        return RoomSummary.builder()
                .roomId(room.getId())
                .roomPicture(room.getRoomPicture())
                .roomTitle(room.getRoomTitle())
                .roomDescription(room.getRoomDescription())
                .build();
    }

}
