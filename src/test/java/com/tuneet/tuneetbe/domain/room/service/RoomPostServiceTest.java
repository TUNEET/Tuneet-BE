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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomPostServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long ROOM_ID = 100L;
    private static final Long POST_ID = 20L;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomPostRepository roomPostRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomUserRepository roomUserRepository;

    @InjectMocks
    private RoomPostService roomPostService;

    private void givenMember() {
        given(roomRepository.findById(ROOM_ID)).willReturn(Optional.of(room()));
        given(roomUserRepository.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(true);
    }

    @Nested
    @DisplayName("방 포스트 작성")
    class CreateRoomPost {

        @Test
        @DisplayName("방, 작성자, 노래, 코멘트를 저장하고 내 포스트로 응답한다")
        void savesPost() {
            // given
            User user = user(USER_ID, "나");
            Music music = music(7L, "Hype Boy");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            givenMember();
            given(roomPostRepository.save(any(RoomPost.class))).willAnswer(invocation -> {
                RoomPost post = invocation.getArgument(0);
                ReflectionTestUtils.setField(post, "id", POST_ID);
                return post;
            });

            // when
            RoomPostResponse response = roomPostService.createRoomPost(ROOM_ID, USER_ID, music, "요즘 계속 듣는 노래");

            // then
            ArgumentCaptor<RoomPost> captor = ArgumentCaptor.forClass(RoomPost.class);
            verify(roomPostRepository).save(captor.capture());
            RoomPost saved = captor.getValue();
            assertThat(saved.getRoom().getId()).isEqualTo(ROOM_ID);
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getMusic()).isSameAs(music);
            assertThat(saved.getRoomPostContent()).isEqualTo("요즘 계속 듣는 노래");

            assertThat(response.getRoomPostId()).isEqualTo(POST_ID);
            assertThat(response.getNickname()).isEqualTo("나");
            assertThat(response.getProfilePicture()).isEqualTo("https://.../profile/1.png");
            assertThat(response.getMusic().getMusicId()).isEqualTo(7L);
            assertThat(response.getIsMine()).isTrue();
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void userNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomPostService.createRoomPost(ROOM_ID, USER_ID, music(7L, "Hype Boy"), "코멘트"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);

            verify(roomPostRepository, never()).save(any());
        }

        @Test
        @DisplayName("방이 없으면 ROOM_NOT_FOUND 예외가 발생한다")
        void roomNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, "나")));
            given(roomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomPostService.createRoomPost(ROOM_ID, USER_ID, music(7L, "Hype Boy"), "코멘트"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.ROOM_NOT_FOUND);

            verify(roomPostRepository, never()).save(any());
        }

        @Test
        @DisplayName("방 멤버가 아니면 NOT_ROOM_MEMBER 예외가 발생하고 저장하지 않는다")
        void notMember() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, "나")));
            given(roomRepository.findById(ROOM_ID)).willReturn(Optional.of(room()));
            given(roomUserRepository.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> roomPostService.createRoomPost(ROOM_ID, USER_ID, music(7L, "Hype Boy"), "코멘트"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.NOT_ROOM_MEMBER);

            verify(roomPostRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("방 포스트 목록 조회")
    class GetRoomPostList {

        @Test
        @DisplayName("cursor가 없으면 최신 포스트부터 size만큼 조회하고 내 포스트만 isMine이 true다")
        void firstPage() {
            // given
            givenMember();
            Pageable pageable = PageRequest.of(0, 2);
            Music music = music(7L, "Hype Boy");
            List<RoomPost> posts = List.of(
                    post(22L, user(OTHER_USER_ID, "멤버"), music),
                    post(21L, user(USER_ID, "나"), music));
            given(roomPostRepository.findByRoomIdOrderByIdDesc(ROOM_ID, pageable))
                    .willReturn(new SliceImpl<>(posts, pageable, true));

            // when
            RoomPostListResponse response = roomPostService.getRoomPostList(ROOM_ID, USER_ID, null, 2);

            // then
            assertThat(response.getRoomPosts())
                    .extracting(RoomPostResponse::getRoomPostId, RoomPostResponse::getNickname, RoomPostResponse::getIsMine)
                    .containsExactly(
                            tuple(22L, "멤버", false),
                            tuple(21L, "나", true));
            assertThat(response.getHasNext()).isTrue();
            verify(roomPostRepository, never()).findByRoomIdAndIdLessThanOrderByIdDesc(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("cursor가 있으면 cursor보다 작은 id의 포스트를 조회한다")
        void nextPage() {
            // given
            givenMember();
            Pageable pageable = PageRequest.of(0, 2);
            given(roomPostRepository.findByRoomIdAndIdLessThanOrderByIdDesc(ROOM_ID, 21L, pageable))
                    .willReturn(new SliceImpl<>(List.of(post(20L, user(USER_ID, "나"), music(7L, "Hype Boy"))), pageable, false));

            // when
            RoomPostListResponse response = roomPostService.getRoomPostList(ROOM_ID, USER_ID, 21L, 2);

            // then
            assertThat(response.getRoomPosts()).extracting(RoomPostResponse::getRoomPostId).containsExactly(20L);
            assertThat(response.getHasNext()).isFalse();
            verify(roomPostRepository, never()).findByRoomIdOrderByIdDesc(anyLong(), any());
        }

        @Test
        @DisplayName("포스트가 없으면 빈 목록과 hasNext false를 반환한다")
        void empty() {
            // given
            givenMember();
            Pageable pageable = PageRequest.of(0, 20);
            given(roomPostRepository.findByRoomIdOrderByIdDesc(ROOM_ID, pageable))
                    .willReturn(new SliceImpl<>(List.of(), pageable, false));

            // when
            RoomPostListResponse response = roomPostService.getRoomPostList(ROOM_ID, USER_ID, null, 20);

            // then
            assertThat(response.getRoomPosts()).isEmpty();
            assertThat(response.getHasNext()).isFalse();
        }

        @Test
        @DisplayName("방이 없으면 ROOM_NOT_FOUND 예외가 발생한다")
        void roomNotFound() {
            // given
            given(roomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomPostService.getRoomPostList(ROOM_ID, USER_ID, null, 20))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("방 멤버가 아니면 NOT_ROOM_MEMBER 예외가 발생하고 포스트를 조회하지 않는다")
        void notMember() {
            // given
            given(roomRepository.findById(ROOM_ID)).willReturn(Optional.of(room()));
            given(roomUserRepository.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> roomPostService.getRoomPostList(ROOM_ID, USER_ID, null, 20))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.NOT_ROOM_MEMBER);

            verify(roomPostRepository, never()).findByRoomIdOrderByIdDesc(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("방 포스트 삭제")
    class DeleteRoomPost {

        @Test
        @DisplayName("내가 쓴 포스트면 삭제한다")
        void deletesMine() {
            // given
            givenMember();
            RoomPost post = post(POST_ID, user(USER_ID, "나"), music(7L, "Hype Boy"));
            given(roomPostRepository.findByIdAndRoomId(POST_ID, ROOM_ID)).willReturn(Optional.of(post));

            // when
            roomPostService.deleteRoomPost(ROOM_ID, POST_ID, USER_ID);

            // then
            verify(roomPostRepository).delete(post);
        }

        @Test
        @DisplayName("포스트가 없거나 다른 방의 포스트면 ROOM_POST_NOT_FOUND 예외가 발생한다")
        void notFoundInRoom() {
            // given
            givenMember();
            given(roomPostRepository.findByIdAndRoomId(POST_ID, ROOM_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomPostService.deleteRoomPost(ROOM_ID, POST_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.ROOM_POST_NOT_FOUND);

            verify(roomPostRepository, never()).delete(any());
        }

        @Test
        @DisplayName("다른 사람이 쓴 포스트면 ROOM_POST_ACCESS_DENIED 예외가 발생하고 삭제하지 않는다")
        void notAuthor() {
            // given
            givenMember();
            RoomPost post = post(POST_ID, user(OTHER_USER_ID, "멤버"), music(7L, "Hype Boy"));
            given(roomPostRepository.findByIdAndRoomId(POST_ID, ROOM_ID)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> roomPostService.deleteRoomPost(ROOM_ID, POST_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.ROOM_POST_ACCESS_DENIED);

            verify(roomPostRepository, never()).delete(any());
        }

        @Test
        @DisplayName("방 멤버가 아니면 NOT_ROOM_MEMBER 예외가 발생하고 포스트를 조회하지 않는다")
        void notMember() {
            // given
            given(roomRepository.findById(ROOM_ID)).willReturn(Optional.of(room()));
            given(roomUserRepository.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> roomPostService.deleteRoomPost(ROOM_ID, POST_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.NOT_ROOM_MEMBER);

            verify(roomPostRepository, never()).findByIdAndRoomId(anyLong(), anyLong());
            verify(roomPostRepository, never()).delete(any());
        }
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

    private User user(Long id, String nickname) {
        User user = User.builder()
                .email(id + "@email.com")
                .password("encoded-password")
                .termsAgreedAt(LocalDateTime.now())
                .privacyAgreedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        ReflectionTestUtils.setField(user, "profilePicture", "https://.../profile/" + id + ".png");
        return user;
    }

    private Music music(Long id, String title) {
        Music music = BeanUtils.instantiateClass(Music.class);
        ReflectionTestUtils.setField(music, "id", id);
        ReflectionTestUtils.setField(music, "title", title);
        ReflectionTestUtils.setField(music, "artist", "NewJeans");
        return music;
    }

    private RoomPost post(Long id, User user, Music music) {
        RoomPost post = RoomPost.builder()
                .room(room())
                .user(user)
                .music(music)
                .roomPostContent("코멘트 " + id)
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        return post;
    }
}
