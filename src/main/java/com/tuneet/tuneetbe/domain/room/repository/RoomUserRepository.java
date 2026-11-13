package com.tuneet.tuneetbe.domain.room.repository;

import com.tuneet.tuneetbe.domain.room.entity.Room;
import com.tuneet.tuneetbe.domain.room.entity.RoomUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomUserRepository extends JpaRepository<RoomUser, Long> {

    // 사용자가 참여 중인 방 멤버 정보 조회
    List<RoomUser> findAllByUserId(Long userId);

    // 방에서 해당 사용자를 제외하고 가장 먼저 참여한 멤버 조회
    Optional<RoomUser> findFirstByRoomIdAndUserIdNotOrderByIdAsc(Long roomId, Long userId);

    // 방의 멤버 정보 전체 삭제
    void deleteAllByRoomId(Long roomId);

    @EntityGraph(attributePaths = "room")
    List<RoomUser> findAllByUserIdOrderByIdDesc(Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<RoomUser> findAllByRoomOrderByIdAsc(Room room);

    @EntityGraph(attributePaths = {"user", "user.profileMusic"})
    List<RoomUser> findAllWithProfileMusicByRoomOrderByIdAsc(Room room);

    // 특정 방에서의 유저 역할 탐색
    Optional<RoomUser> findByRoomIdAndUserId(Long roomId, Long userId);
}
