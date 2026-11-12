package com.tuneet.tuneetbe.domain.room.service;

import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomPostListResponse;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomPostResponse;
import com.tuneet.tuneetbe.domain.room.entity.Room;
import com.tuneet.tuneetbe.domain.room.entity.RoomPost;
import com.tuneet.tuneetbe.domain.room.exception.RoomErrorCode;
import com.tuneet.tuneetbe.domain.room.repository.RoomPostRepository;
import com.tuneet.tuneetbe.domain.room.repository.RoomRepository;
import com.tuneet.tuneetbe.domain.room.repository.RoomUserRepository;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoomPostService {

    private final RoomRepository roomRepository;
    private final RoomPostRepository roomPostRepository;
    private final UserRepository userRepository;
    private final RoomUserRepository roomUserRepository;

    @Transactional
    public RoomPostResponse createRoomPost(Long roomId, Long userId, Music music, String content){

        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        Room room = getRoomAsMember(roomId, userId);

        RoomPost roomPost = RoomPost.builder()
                .room(room)
                .user(user)
                .music(music)
                .roomPostContent(content)
                .build();

        RoomPost savedRoomPost = roomPostRepository.save(roomPost);

        log.info("[RoomPostService] 방 포스트 작성 성공: roomPostId = {}", savedRoomPost.getId());

        return RoomPostResponse.from(savedRoomPost, userId);
    }

    public RoomPostListResponse getRoomPostList(Long roomId, Long userId, Long cursor, Integer size){

        getRoomAsMember(roomId, userId);

        Pageable pageable = PageRequest.of(0, size);   // 페이지는 항상 0, 위치는 cursor가 정함
        Slice<RoomPost> posts = (cursor == null)
                ? roomPostRepository.findByRoomIdOrderByIdDesc(roomId, pageable)
                : roomPostRepository.findByRoomIdAndIdLessThanOrderByIdDesc(roomId, cursor, pageable);

        return RoomPostListResponse.from(posts, userId);
    }

    @Transactional
    public void deleteRoomPost(Long roomId, Long postId, Long userId) {

        getRoomAsMember(roomId, userId);

        RoomPost roomPost = roomPostRepository.findByIdAndRoomId(postId, roomId)
                .orElseThrow(() -> new CustomException(RoomErrorCode.ROOM_POST_NOT_FOUND));

        if (!roomPost.getUser().getId().equals(userId)) {
            throw new CustomException(RoomErrorCode.ROOM_POST_ACCESS_DENIED);
        }

        roomPostRepository.delete(roomPost);

        log.info("[RoomPostService] 방 포스트 삭제 성공: roomPostId = {}", postId);
    }

    // 방이 있고 내가 멤버인지 확인
    private Room getRoomAsMember(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new CustomException(RoomErrorCode.ROOM_NOT_FOUND));
        if (!roomUserRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new CustomException(RoomErrorCode.NOT_ROOM_MEMBER);
        }
        return room;
    }

}
