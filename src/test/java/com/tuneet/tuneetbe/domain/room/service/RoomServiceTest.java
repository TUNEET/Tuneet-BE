package com.tuneet.tuneetbe.domain.room.service;

import com.tuneet.tuneetbe.domain.room.dto.request.RoomCreateRequest;
import com.tuneet.tuneetbe.domain.room.dto.request.RoomEntryRequest;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomCreateResponse;
import com.tuneet.tuneetbe.domain.room.dto.response.RoomEntryResponse;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 100L;
    private static final String INVITE_CODE = "aB3kP9xQ";

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomUserRepository roomUserRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private InviteCodeGenerator inviteCodeGenerator;

    @InjectMocks
    private RoomService roomService;

    private User savedUser() {
        User user = User.builder()
                .email("test01@email.com")
                .password("encoded-password")
                .termsAgreedAt(LocalDateTime.now())
                .privacyAgreedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private RoomCreateRequest request(String password) {
        RoomCreateRequest request = BeanUtils.instantiateClass(RoomCreateRequest.class);
        ReflectionTestUtils.setField(request, "roomTitle", "퇴근길 플리 모임");
        ReflectionTestUtils.setField(request, "roomDescription", "퇴근길에 듣는 노래를 공유해요");
        ReflectionTestUtils.setField(request, "roomPicture", "https://.../room/abc.png");
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private void givenRoomSaveAssignsId() {
        given(roomRepository.save(any(Room.class))).willAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            ReflectionTestUtils.setField(room, "id", ROOM_ID);
            return room;
        });
    }

    private RoomUser roomUser(Long roomId, String title, RoomRole role) {
        Room room = Room.builder()
                .roomTitle(title)
                .roomDescription(title + " 소개")
                .inviteCode("code" + roomId)
                .build();
        ReflectionTestUtils.setField(room, "id", roomId);
        return RoomUser.builder().room(room).user(savedUser()).role(role).build();
    }

    @Nested
    @DisplayName("방 생성")
    class CreateRoom {

        @Test
        @DisplayName("방을 저장하고 만든 사용자를 OWNER로 함께 저장한다")
        void savesOwner() {
            // given
            User user = savedUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(inviteCodeGenerator.generate(8)).willReturn(INVITE_CODE);
            given(roomRepository.existsByInviteCode(INVITE_CODE)).willReturn(false);
            givenRoomSaveAssignsId();

            // when
            RoomCreateResponse response = roomService.createRoom(request(null), USER_ID);

            // then
            ArgumentCaptor<RoomUser> captor = ArgumentCaptor.forClass(RoomUser.class);
            verify(roomUserRepository).save(captor.capture());
            RoomUser owner = captor.getValue();
            assertThat(owner.getUser()).isSameAs(user);
            assertThat(owner.getRoom().getId()).isEqualTo(ROOM_ID);
            assertThat(owner.getRole()).isEqualTo(RoomRole.OWNER);

            assertThat(response.getRoomId()).isEqualTo(ROOM_ID);
            assertThat(response.getRoomTitle()).isEqualTo("퇴근길 플리 모임");
            assertThat(response.getRoomDescription()).isEqualTo("퇴근길에 듣는 노래를 공유해요");
            assertThat(response.getRoomPicture()).isEqualTo("https://.../room/abc.png");
            assertThat(response.getInviteCode()).isEqualTo(INVITE_CODE);
        }

        @Test
        @DisplayName("비밀번호가 있으면 해시해서 저장하고 hasPassword는 true다")
        void withPassword() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(savedUser()));
            given(inviteCodeGenerator.generate(8)).willReturn(INVITE_CODE);
            given(roomRepository.existsByInviteCode(INVITE_CODE)).willReturn(false);
            given(passwordEncoder.encode("1234")).willReturn("hashed-1234");
            givenRoomSaveAssignsId();

            // when
            RoomCreateResponse response = roomService.createRoom(request("1234"), USER_ID);

            // then
            ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
            verify(roomRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("hashed-1234");
            assertThat(response.getHasPassword()).isTrue();
        }

        @Test
        @DisplayName("비밀번호가 없으면 null로 저장하고 hasPassword는 false다")
        void withoutPassword() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(savedUser()));
            given(inviteCodeGenerator.generate(8)).willReturn(INVITE_CODE);
            given(roomRepository.existsByInviteCode(INVITE_CODE)).willReturn(false);
            givenRoomSaveAssignsId();

            // when
            RoomCreateResponse response = roomService.createRoom(request(null), USER_ID);

            // then
            ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
            verify(roomRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isNull();
            assertThat(response.getHasPassword()).isFalse();
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("초대 코드가 겹치면 새로 만들어 겹치지 않는 코드를 쓴다")
        void retriesDuplicateInviteCode() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(savedUser()));
            given(inviteCodeGenerator.generate(8)).willReturn("DUPLICAT", INVITE_CODE);
            given(roomRepository.existsByInviteCode("DUPLICAT")).willReturn(true);
            given(roomRepository.existsByInviteCode(INVITE_CODE)).willReturn(false);
            givenRoomSaveAssignsId();

            // when
            RoomCreateResponse response = roomService.createRoom(request(null), USER_ID);

            // then
            assertThat(response.getInviteCode()).isEqualTo(INVITE_CODE);
        }

        @Test
        @DisplayName("초대 코드가 5번 모두 겹치면 INVITE_CODE_GENERATION_FAILED 예외가 발생하고 저장하지 않는다")
        void inviteCodeGenerationFailed() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(savedUser()));
            given(inviteCodeGenerator.generate(anyInt())).willReturn("DUPLICAT");
            given(roomRepository.existsByInviteCode(anyString())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> roomService.createRoom(request(null), USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.INVITE_CODE_GENERATION_FAILED);

            verify(inviteCodeGenerator, times(5)).generate(8);
            verify(roomRepository, never()).save(any());
            verify(roomUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생하고 저장하지 않는다")
        void userNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomService.createRoom(request(null), USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);

            verify(roomRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("내 방 목록 조회")
    class GetRooms {

        @Test
        @DisplayName("내가 참여한 방만 최근 참여순 그대로 반환한다")
        void myRoomsInOrder() {
            // given
            given(userRepository.existsById(USER_ID)).willReturn(true);
            given(roomUserRepository.findAllByUserIdOrderByIdDesc(USER_ID)).willReturn(List.of(
                    roomUser(2L, "최근에 들어간 방", RoomRole.MEMBER),
                    roomUser(1L, "내가 만든 방", RoomRole.OWNER)
            ));

            // when
            RoomSummaryList response = roomService.getRoomSummaryList(USER_ID);

            // then
            verify(roomUserRepository).findAllByUserIdOrderByIdDesc(USER_ID);
            assertThat(response.getRooms())
                    .extracting("roomId", "roomTitle", "roomDescription")
                    .containsExactly(
                            tuple(2L, "최근에 들어간 방", "최근에 들어간 방 소개"),
                            tuple(1L, "내가 만든 방", "내가 만든 방 소개")
                    );
        }

        @Test
        @DisplayName("참여한 방이 없으면 null이 아닌 빈 목록을 반환한다")
        void emptyList() {
            // given
            given(userRepository.existsById(USER_ID)).willReturn(true);
            given(roomUserRepository.findAllByUserIdOrderByIdDesc(USER_ID)).willReturn(List.of());

            // when
            RoomSummaryList response = roomService.getRoomSummaryList(USER_ID);

            // then
            assertThat(response.getRooms()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void userNotFound() {
            // given
            given(userRepository.existsById(USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> roomService.getRoomSummaryList(USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);

            verify(roomUserRepository, never()).findAllByUserIdOrderByIdDesc(any());
        }
    }

    @Nested
    @DisplayName("방 입장")
    class EnterRoom {

        private static final String HASHED = "hashed-1234";

        private Room room(String password) {
            Room room = Room.builder()
                    .roomTitle("퇴근길 플리 모임")
                    .password(password)
                    .inviteCode(INVITE_CODE)
                    .build();
            ReflectionTestUtils.setField(room, "id", ROOM_ID);
            return room;
        }

        private RoomEntryRequest entry(String password) {
            RoomEntryRequest request = BeanUtils.instantiateClass(RoomEntryRequest.class);
            ReflectionTestUtils.setField(request, "inviteCode", INVITE_CODE);
            ReflectionTestUtils.setField(request, "password", password);
            return request;
        }

        private void givenUserAndRoom(Room room) {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(savedUser()));
            given(roomRepository.findByInviteCode(INVITE_CODE)).willReturn(Optional.of(room));
        }

        @Test
        @DisplayName("공개 방이면 비밀번호 없이 MEMBER로 저장한다")
        void publicRoom() {
            // given
            givenUserAndRoom(room(null));
            given(roomUserRepository.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(false);

            // when
            RoomEntryResponse response = roomService.entryRoom(entry(null), USER_ID);

            // then
            ArgumentCaptor<RoomUser> captor = ArgumentCaptor.forClass(RoomUser.class);
            verify(roomUserRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(RoomRole.MEMBER);
            assertThat(captor.getValue().getUser().getId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getRoom().getId()).isEqualTo(ROOM_ID);
            assertThat(response.getRoomId()).isEqualTo(ROOM_ID);
            assertThat(response.isAlreadyJoined()).isFalse();
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("공개 방에 비밀번호를 보내도 무시하고 입장한다")
        void publicRoomIgnoresPassword() {
            // given
            givenUserAndRoom(room(null));
            given(roomUserRepository.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(false);

            // when
            RoomEntryResponse response = roomService.entryRoom(entry("1234"), USER_ID);

            // then
            assertThat(response.isAlreadyJoined()).isFalse();
            verify(roomUserRepository).save(any(RoomUser.class));
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("잠긴 방에 비밀번호가 맞으면 입장한다")
        void lockedRoomCorrectPassword() {
            // given
            givenUserAndRoom(room(HASHED));
            given(roomUserRepository.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(false);
            given(passwordEncoder.matches("1234", HASHED)).willReturn(true);

            // when
            RoomEntryResponse response = roomService.entryRoom(entry("1234"), USER_ID);

            // then
            assertThat(response.isAlreadyJoined()).isFalse();
            verify(roomUserRepository).save(any(RoomUser.class));
        }

        @Test
        @DisplayName("잠긴 방에 비밀번호 없이 요청하면 ROOM_PASSWORD_REQUIRED 예외가 발생하고 저장하지 않는다")
        void lockedRoomWithoutPassword() {
            // given
            givenUserAndRoom(room(HASHED));
            given(roomUserRepository.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> roomService.entryRoom(entry(null), USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.ROOM_PASSWORD_REQUIRED);

            verify(roomUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("잠긴 방에 비밀번호가 틀리면 ROOM_PASSWORD_MISMATCH 예외가 발생하고 저장하지 않는다")
        void lockedRoomWrongPassword() {
            // given
            givenUserAndRoom(room(HASHED));
            given(roomUserRepository.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(false);
            given(passwordEncoder.matches("9999", HASHED)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> roomService.entryRoom(entry("9999"), USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.ROOM_PASSWORD_MISMATCH);

            verify(roomUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 멤버면 비밀번호 확인 없이 alreadyJoined=true를 반환하고 저장하지 않는다")
        void alreadyJoined() {
            // given
            givenUserAndRoom(room(HASHED));
            given(roomUserRepository.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(true);

            // when
            RoomEntryResponse response = roomService.entryRoom(entry(null), USER_ID);

            // then
            assertThat(response.getRoomId()).isEqualTo(ROOM_ID);
            assertThat(response.isAlreadyJoined()).isTrue();
            verify(roomUserRepository, never()).save(any());
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("초대 코드에 맞는 방이 없으면 ROOM_NOT_FOUND 예외가 발생한다")
        void roomNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(savedUser()));
            given(roomRepository.findByInviteCode(INVITE_CODE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomService.entryRoom(entry(null), USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.ROOM_NOT_FOUND);

            verify(roomUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void userNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomService.entryRoom(entry(null), USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);

            verify(roomRepository, never()).findByInviteCode(any());
        }
    }
}
