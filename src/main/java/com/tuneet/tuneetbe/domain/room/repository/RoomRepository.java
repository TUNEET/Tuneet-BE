package com.tuneet.tuneetbe.domain.room.repository;

import com.tuneet.tuneetbe.domain.room.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByInviteCode(String inviteCode);

    Optional<Room> findByInviteCode(String inviteCode);

    // 행 잠금을 걸고 방 조회 (같은 방의 나가기, 삭제를 순서대로 처리)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :roomId")
    Optional<Room> findByIdForUpdate(@Param("roomId") Long roomId);

    // 방 삭제 (쿼리 1번, 멤버·글·플레이리스트를 벌크로 지운 뒤 사용)
    @Modifying
    @Query("delete from Room r where r.id = :roomId")
    void deleteRoomById(@Param("roomId") Long roomId);
}
