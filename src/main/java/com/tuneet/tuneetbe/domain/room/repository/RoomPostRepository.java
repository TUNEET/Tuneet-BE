package com.tuneet.tuneetbe.domain.room.repository;

import com.tuneet.tuneetbe.domain.room.entity.RoomPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomPostRepository extends JpaRepository<RoomPost, Long> {

    // 방의 글 전체 삭제
    void deleteAllByRoomId(Long roomId);
}
