package com.tuneet.tuneetbe.domain.room.repository;

import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.room.entity.Room;
import com.tuneet.tuneetbe.domain.room.entity.RoomPlaylist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomPlaylistRepository extends JpaRepository<RoomPlaylist, Long> {

    // 방의 플레이리스트 전체 삭제
    void deleteAllByRoomId(Long roomId);

    // 방의 플레이리스트 전체 삭제 (쿼리 1번)
    @Modifying
    @Query("delete from RoomPlaylist rp where rp.room.id = :roomId")
    void bulkDeleteByRoomId(@Param("roomId") Long roomId);

    @EntityGraph(attributePaths = "music")
    List<RoomPlaylist> findTop2ByRoomOrderByPositionAscIdAsc(Room room);

    @Query("select coalesce(max(rp.position), 0) from RoomPlaylist rp where rp.room.id = :roomId")
    int findMaxPosition(@Param("roomId") Long roomId);

    @EntityGraph(attributePaths = "music")
    List<RoomPlaylist> findByRoomOrderByPositionAscIdAsc(Room room);

    boolean existsByRoomAndMusic(Room room, Music music);

    @EntityGraph(attributePaths = "music")
    Optional<RoomPlaylist> findByRoomAndId(Room room, Long id);

    @EntityGraph(attributePaths = "music")
    List<RoomPlaylist> findByRoomOrderByIdAsc(Room room);
}
