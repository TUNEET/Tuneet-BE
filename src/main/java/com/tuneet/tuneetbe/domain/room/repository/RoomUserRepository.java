package com.tuneet.tuneetbe.domain.room.repository;

import com.tuneet.tuneetbe.domain.room.entity.Room;
import com.tuneet.tuneetbe.domain.room.entity.RoomUser;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoomUserRepository extends JpaRepository<RoomUser, Long> {

    // 사용자가 참여 중인 방 id 조회 (방 id 오름차순)
    @Query("select ru.room.id from RoomUser ru where ru.user.id = :userId order by ru.room.id")
    List<Long> findRoomIdsByUserId(@Param("userId") Long userId);

    // 행 잠금을 걸고 방에서 해당 사용자를 제외하고 가장 먼저 참여한 멤버 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RoomUser> findFirstByRoomIdAndUserIdNotOrderByIdAsc(Long roomId, Long userId);

    // 행 잠금을 걸고 특정 방에서의 유저 역할 탐색
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ru from RoomUser ru where ru.room.id = :roomId and ru.user.id = :userId")
    Optional<RoomUser> findByRoomIdAndUserIdForUpdate(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // 방의 멤버 정보 전체 삭제 (쿼리 1번)
    @Modifying
    @Query("delete from RoomUser ru where ru.room.id = :roomId")
    void deleteAllByRoomId(@Param("roomId") Long roomId);

    @EntityGraph(attributePaths = "room")
    List<RoomUser> findAllByUserIdOrderByIdDesc(Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<RoomUser> findAllByRoomOrderByIdAsc(Room room);

    @EntityGraph(attributePaths = {"user", "user.profileMusic"})
    List<RoomUser> findAllWithProfileMusicByRoomOrderByIdAsc(Room room);

    // 방에 있는 멤버들의 userId 조회
    @Query("select ru.user.id from RoomUser ru where ru.room.id = :roomId")
    Set<Long> findUserIdsByRoomId(@Param("roomId") Long roomId);

    // 특정 방에서의 유저 역할 탐색
    Optional<RoomUser> findByRoomIdAndUserId(Long roomId, Long userId);
}
