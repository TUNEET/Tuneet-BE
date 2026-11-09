package com.tuneet.tuneetbe.domain.room.repository;

import com.tuneet.tuneetbe.domain.room.entity.RoomPlaylist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomPlaylistRepository extends JpaRepository<RoomPlaylist, Long> {

    // 방의 플레이리스트 전체 삭제
    void deleteAllByRoomId(Long roomId);
}
