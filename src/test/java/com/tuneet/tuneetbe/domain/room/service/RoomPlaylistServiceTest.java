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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
class RoomPlaylistServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long ROOM_ID = 100L;
    private static final Long PLAYLIST_ID = 30L;

    @Mock
    private RoomPlaylistRepository roomPlaylistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomMemberReader roomMemberReader;

    @InjectMocks
    private RoomPlaylistService roomPlaylistService;

    private Room givenRoomAsMember() {
        Room room = room();
        given(roomMemberReader.getRoomAsMember(ROOM_ID, USER_ID)).willReturn(room);
        return room;
    }

    private Room givenMember(RoomRole role) {
        Room room = room();
        given(roomMemberReader.getMember(ROOM_ID, USER_ID)).willReturn(roomUser(room, user(USER_ID, "나"), role));
        return room;
    }

    @Nested
    @DisplayName("방 플레이리스트 곡 추가")
    class CreateRoomPlaylist {

        @Test
        @DisplayName("마지막 position 다음 순서로 저장하고 응답한다")
        void savesAtLast() {
            // given
            User user = user(USER_ID, "나");
            Music music = music(7L, "Hype Boy");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            Room room = givenRoomAsMember();
            given(roomPlaylistRepository.existsByRoomAndMusic(room, music)).willReturn(false);
            given(roomPlaylistRepository.findMaxPosition(ROOM_ID)).willReturn(3);
            given(roomPlaylistRepository.saveAndFlush(any(RoomPlaylist.class))).willAnswer(invocation -> {
                RoomPlaylist playlist = invocation.getArgument(0);
                ReflectionTestUtils.setField(playlist, "id", PLAYLIST_ID);
                return playlist;
            });

            // when
            RoomPlaylistResponse response = roomPlaylistService.createRoomPlaylist(ROOM_ID, USER_ID, music);

            // then
            ArgumentCaptor<RoomPlaylist> captor = ArgumentCaptor.forClass(RoomPlaylist.class);
            verify(roomPlaylistRepository).saveAndFlush(captor.capture());
            RoomPlaylist saved = captor.getValue();
            assertThat(saved.getRoom()).isSameAs(room);
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getMusic()).isSameAs(music);
            assertThat(saved.getPosition()).isEqualTo(4);

            assertThat(response.getRoomPlaylistId()).isEqualTo(PLAYLIST_ID);
            assertThat(response.getPosition()).isEqualTo(4);
            assertThat(response.getMusic().getMusicId()).isEqualTo("music-7");
        }

        @Test
        @DisplayName("플레이리스트가 비어 있으면 position 1로 저장한다")
        void firstSong() {
            // given
            Music music = music(7L, "Hype Boy");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, "나")));
            Room room = givenRoomAsMember();
            given(roomPlaylistRepository.existsByRoomAndMusic(room, music)).willReturn(false);
            given(roomPlaylistRepository.findMaxPosition(ROOM_ID)).willReturn(0);
            given(roomPlaylistRepository.saveAndFlush(any(RoomPlaylist.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            RoomPlaylistResponse response = roomPlaylistService.createRoomPlaylist(ROOM_ID, USER_ID, music);

            // then
            assertThat(response.getPosition()).isEqualTo(1);
        }

        @Test
        @DisplayName("같은 방에 이미 있는 곡이면 ROOM_PLAYLIST_DUPLICATE_MUSIC 예외가 발생하고 저장하지 않는다")
        void duplicateMusic() {
            // given
            Music music = music(7L, "Hype Boy");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, "나")));
            Room room = givenRoomAsMember();
            given(roomPlaylistRepository.existsByRoomAndMusic(room, music)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> roomPlaylistService.createRoomPlaylist(ROOM_ID, USER_ID, music))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.ROOM_PLAYLIST_DUPLICATE_MUSIC);

            verify(roomPlaylistRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("같은 곡이 동시에 추가되어 유니크 제약에 걸리면 ROOM_PLAYLIST_DUPLICATE_MUSIC 예외로 바꾼다")
        void duplicateMusicConcurrently() {
            // given
            Music music = music(7L, "Hype Boy");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, "나")));
            Room room = givenRoomAsMember();
            given(roomPlaylistRepository.existsByRoomAndMusic(room, music)).willReturn(false);
            given(roomPlaylistRepository.findMaxPosition(ROOM_ID)).willReturn(3);
            given(roomPlaylistRepository.saveAndFlush(any(RoomPlaylist.class)))
                    .willThrow(new DataIntegrityViolationException("uk_room_playlists_room_music"));

            // when & then
            assertThatThrownBy(() -> roomPlaylistService.createRoomPlaylist(ROOM_ID, USER_ID, music))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.ROOM_PLAYLIST_DUPLICATE_MUSIC);
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void userNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomPlaylistService.createRoomPlaylist(ROOM_ID, USER_ID, music(7L, "Hype Boy")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);

            verify(roomPlaylistRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("방이 없으면 ROOM_NOT_FOUND 예외가 발생한다")
        void roomNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, "나")));
            given(roomMemberReader.getRoomAsMember(ROOM_ID, USER_ID))
                    .willThrow(new CustomException(RoomErrorCode.ROOM_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> roomPlaylistService.createRoomPlaylist(ROOM_ID, USER_ID, music(7L, "Hype Boy")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.ROOM_NOT_FOUND);

            verify(roomPlaylistRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("방 멤버가 아니면 NOT_ROOM_MEMBER 예외가 발생하고 저장하지 않는다")
        void notMember() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, "나")));
            given(roomMemberReader.getRoomAsMember(ROOM_ID, USER_ID))
                    .willThrow(new CustomException(RoomErrorCode.NOT_ROOM_MEMBER));

            // when & then
            assertThatThrownBy(() -> roomPlaylistService.createRoomPlaylist(ROOM_ID, USER_ID, music(7L, "Hype Boy")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.NOT_ROOM_MEMBER);

            verify(roomPlaylistRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("방 플레이리스트 전체 조회")
    class GetAllRoomPlaylists {

        @Test
        @DisplayName("position 순으로 조회한 목록을 그대로 응답한다")
        void returnsInOrder() {
            // given
            Room room = givenRoomAsMember();
            User user = user(USER_ID, "나");
            given(roomPlaylistRepository.findByRoomOrderByPositionAscIdAsc(room)).willReturn(List.of(
                    playlist(31L, room, user, music(7L, "Hype Boy"), 1),
                    playlist(32L, room, user, music(8L, "Ditto"), 2)));

            // when
            RoomPlaylistListResponse response = roomPlaylistService.getAllRoomPlaylists(ROOM_ID, USER_ID);

            // then
            assertThat(response.getPlaylists())
                    .extracting(RoomPlaylistResponse::getRoomPlaylistId, RoomPlaylistResponse::getPosition)
                    .containsExactly(tuple(31L, 1), tuple(32L, 2));
        }

        @Test
        @DisplayName("방 멤버가 아니면 NOT_ROOM_MEMBER 예외가 발생하고 목록을 조회하지 않는다")
        void notMember() {
            // given
            given(roomMemberReader.getRoomAsMember(ROOM_ID, USER_ID))
                    .willThrow(new CustomException(RoomErrorCode.NOT_ROOM_MEMBER));

            // when & then
            assertThatThrownBy(() -> roomPlaylistService.getAllRoomPlaylists(ROOM_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.NOT_ROOM_MEMBER);

            verify(roomPlaylistRepository, never()).findByRoomOrderByPositionAscIdAsc(any());
        }
    }

    @Nested
    @DisplayName("방 플레이리스트 곡 삭제")
    class DeleteRoomPlaylist {

        @Test
        @DisplayName("곡을 추가한 사람은 삭제할 수 있다")
        void deletedByAdder() {
            // given
            Room room = givenMember(RoomRole.MEMBER);
            RoomPlaylist playlist = playlist(PLAYLIST_ID, room, user(USER_ID, "나"), music(7L, "Hype Boy"), 1);
            given(roomPlaylistRepository.findByRoomAndId(room, PLAYLIST_ID)).willReturn(Optional.of(playlist));

            // when
            roomPlaylistService.deleteRoomPlaylist(ROOM_ID, PLAYLIST_ID, USER_ID);

            // then
            verify(roomPlaylistRepository).delete(playlist);
        }

        @Test
        @DisplayName("방장은 다른 사람이 추가한 곡도 삭제할 수 있다")
        void deletedByOwner() {
            // given
            Room room = givenMember(RoomRole.OWNER);
            RoomPlaylist playlist = playlist(PLAYLIST_ID, room, user(OTHER_USER_ID, "다른 사람"), music(7L, "Hype Boy"), 1);
            given(roomPlaylistRepository.findByRoomAndId(room, PLAYLIST_ID)).willReturn(Optional.of(playlist));

            // when
            roomPlaylistService.deleteRoomPlaylist(ROOM_ID, PLAYLIST_ID, USER_ID);

            // then
            verify(roomPlaylistRepository).delete(playlist);
        }

        @Test
        @DisplayName("추가한 사람도 방장도 아니면 ROOM_PLAYLIST_ACCESS_DENIED 예외가 발생하고 삭제하지 않는다")
        void accessDenied() {
            // given
            Room room = givenMember(RoomRole.MEMBER);
            RoomPlaylist playlist = playlist(PLAYLIST_ID, room, user(OTHER_USER_ID, "다른 사람"), music(7L, "Hype Boy"), 1);
            given(roomPlaylistRepository.findByRoomAndId(room, PLAYLIST_ID)).willReturn(Optional.of(playlist));

            // when & then
            assertThatThrownBy(() -> roomPlaylistService.deleteRoomPlaylist(ROOM_ID, PLAYLIST_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.ROOM_PLAYLIST_ACCESS_DENIED);

            verify(roomPlaylistRepository, never()).delete(any());
        }

        @Test
        @DisplayName("곡이 없거나 다른 방의 곡이면 ROOM_PLAYLIST_NOT_FOUND 예외가 발생한다")
        void notFound() {
            // given
            Room room = givenMember(RoomRole.OWNER);
            given(roomPlaylistRepository.findByRoomAndId(room, PLAYLIST_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomPlaylistService.deleteRoomPlaylist(ROOM_ID, PLAYLIST_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.ROOM_PLAYLIST_NOT_FOUND);

            verify(roomPlaylistRepository, never()).delete(any());
        }

        @Test
        @DisplayName("방 멤버가 아니면 NOT_ROOM_MEMBER 예외가 발생하고 곡을 조회하지 않는다")
        void notMember() {
            // given
            given(roomMemberReader.getMember(ROOM_ID, USER_ID))
                    .willThrow(new CustomException(RoomErrorCode.NOT_ROOM_MEMBER));

            // when & then
            assertThatThrownBy(() -> roomPlaylistService.deleteRoomPlaylist(ROOM_ID, PLAYLIST_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.NOT_ROOM_MEMBER);

            verify(roomPlaylistRepository, never()).findByRoomAndId(any(), anyLong());
        }
    }

    @Nested
    @DisplayName("방 플레이리스트 전체 삭제")
    class DeleteAllRoomPlaylists {

        @Test
        @DisplayName("방장은 방의 플레이리스트를 한 번에 삭제한다")
        void deletedByOwner() {
            // given
            givenMember(RoomRole.OWNER);

            // when
            roomPlaylistService.deleteAllRoomPlaylists(ROOM_ID, USER_ID);

            // then
            verify(roomPlaylistRepository).deleteAllByRoomId(ROOM_ID);
        }

        @Test
        @DisplayName("방장이 아니면 ROOM_OWNER_ONLY 예외가 발생하고 삭제하지 않는다")
        void notOwner() {
            // given
            givenMember(RoomRole.MEMBER);

            // when & then
            assertThatThrownBy(() -> roomPlaylistService.deleteAllRoomPlaylists(ROOM_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.ROOM_OWNER_ONLY);

            verify(roomPlaylistRepository, never()).deleteAllByRoomId(anyLong());
        }
    }

    @Nested
    @DisplayName("방 플레이리스트 순서 변경")
    class UpdateRoomPlaylistPos {

        @Test
        @DisplayName("보낸 순서대로 position을 1부터 다시 매기고 바뀐 순서로 응답한다")
        void reorders() {
            // given
            Room room = givenRoomAsMember();
            User user = user(USER_ID, "나");
            RoomPlaylist first = playlist(31L, room, user, music(7L, "Hype Boy"), 1);
            RoomPlaylist second = playlist(32L, room, user, music(8L, "Ditto"), 2);
            RoomPlaylist third = playlist(33L, room, user, music(9L, "OMG"), 3);
            given(roomPlaylistRepository.findByRoomOrderByIdAsc(room))
                    .willReturn(new ArrayList<>(List.of(first, second, third)));

            // when
            RoomPlaylistListResponse response = roomPlaylistService.updateRoomPlaylistPos(ROOM_ID, USER_ID, orderRequest(List.of(33L, 31L, 32L)));

            // then
            assertThat(third.getPosition()).isEqualTo(1);
            assertThat(first.getPosition()).isEqualTo(2);
            assertThat(second.getPosition()).isEqualTo(3);
            assertThat(response.getPlaylists())
                    .extracting(RoomPlaylistResponse::getRoomPlaylistId, RoomPlaylistResponse::getPosition)
                    .containsExactly(tuple(33L, 1), tuple(31L, 2), tuple(32L, 3));
        }

        @Test
        @DisplayName("겹친 position도 1부터 다시 매겨 정리한다")
        void fixesDuplicatedPositions() {
            // given
            Room room = givenRoomAsMember();
            User user = user(USER_ID, "나");
            RoomPlaylist first = playlist(31L, room, user, music(7L, "Hype Boy"), 4);
            RoomPlaylist second = playlist(32L, room, user, music(8L, "Ditto"), 4);
            given(roomPlaylistRepository.findByRoomOrderByIdAsc(room))
                    .willReturn(new ArrayList<>(List.of(first, second)));

            // when
            roomPlaylistService.updateRoomPlaylistPos(ROOM_ID, USER_ID, orderRequest(List.of(31L, 32L)));

            // then
            assertThat(first.getPosition()).isEqualTo(1);
            assertThat(second.getPosition()).isEqualTo(2);
        }

        @Test
        @DisplayName("현재 목록보다 곡이 적으면 ROOM_PLAYLIST_ORDER_MISMATCH 예외가 발생한다")
        void missingId() {
            assertOrderMismatch(List.of(31L));
        }

        @Test
        @DisplayName("같은 id가 두 번 들어 있으면 ROOM_PLAYLIST_ORDER_MISMATCH 예외가 발생한다")
        void duplicatedId() {
            assertOrderMismatch(List.of(31L, 31L));
        }

        @Test
        @DisplayName("이 방에 없는 id가 섞여 있으면 ROOM_PLAYLIST_ORDER_MISMATCH 예외가 발생한다")
        void unknownId() {
            assertOrderMismatch(List.of(31L, 99L));
        }

        @Test
        @DisplayName("방 멤버가 아니면 NOT_ROOM_MEMBER 예외가 발생하고 목록을 조회하지 않는다")
        void notMember() {
            // given
            given(roomMemberReader.getRoomAsMember(ROOM_ID, USER_ID))
                    .willThrow(new CustomException(RoomErrorCode.NOT_ROOM_MEMBER));

            // when & then
            assertThatThrownBy(() -> roomPlaylistService.updateRoomPlaylistPos(ROOM_ID, USER_ID, orderRequest(List.of(31L))))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.NOT_ROOM_MEMBER);

            verify(roomPlaylistRepository, never()).findByRoomOrderByIdAsc(any());
        }

        private void assertOrderMismatch(List<Long> ids) {
            // given
            Room room = givenRoomAsMember();
            User user = user(USER_ID, "나");
            RoomPlaylist first = playlist(31L, room, user, music(7L, "Hype Boy"), 1);
            RoomPlaylist second = playlist(32L, room, user, music(8L, "Ditto"), 2);
            given(roomPlaylistRepository.findByRoomOrderByIdAsc(room))
                    .willReturn(new ArrayList<>(List.of(first, second)));

            // when & then
            assertThatThrownBy(() -> roomPlaylistService.updateRoomPlaylistPos(ROOM_ID, USER_ID, orderRequest(ids)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(RoomErrorCode.ROOM_PLAYLIST_ORDER_MISMATCH);

            assertThat(first.getPosition()).isEqualTo(1);
            assertThat(second.getPosition()).isEqualTo(2);
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
        return user;
    }

    private RoomUser roomUser(Room room, User user, RoomRole role) {
        return RoomUser.builder()
                .room(room)
                .user(user)
                .role(role)
                .build();
    }

    private Music music(Long id, String title) {
        Music music = BeanUtils.instantiateClass(Music.class);
        ReflectionTestUtils.setField(music, "id", id);
        ReflectionTestUtils.setField(music, "externalId", "music-" + id);
        ReflectionTestUtils.setField(music, "title", title);
        ReflectionTestUtils.setField(music, "artist", "NewJeans");
        return music;
    }

    private RoomPlaylist playlist(Long id, Room room, User user, Music music, int position) {
        RoomPlaylist playlist = RoomPlaylist.builder()
                .room(room)
                .user(user)
                .music(music)
                .position(position)
                .build();
        ReflectionTestUtils.setField(playlist, "id", id);
        return playlist;
    }

    private RoomPlaylistPosUpdateRequest orderRequest(List<Long> ids) {
        RoomPlaylistPosUpdateRequest request = BeanUtils.instantiateClass(RoomPlaylistPosUpdateRequest.class);
        ReflectionTestUtils.setField(request, "playListIdList", ids);
        return request;
    }
}
