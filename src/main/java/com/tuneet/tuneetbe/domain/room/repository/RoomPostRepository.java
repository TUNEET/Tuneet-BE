package com.tuneet.tuneetbe.domain.room.repository;

import com.tuneet.tuneetbe.domain.room.entity.Room;
import com.tuneet.tuneetbe.domain.room.entity.RoomPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomPostRepository extends JpaRepository<RoomPost, Long> {

    // 방의 글 전체 삭제
    void deleteAllByRoomId(Long roomId);

    @EntityGraph(attributePaths = {"user", "music"})
    List<RoomPost> findTop2ByRoomOrderByCreatedAtDescIdDesc(Room room);

    @EntityGraph(attributePaths = {"user", "music"})
    Slice<RoomPost> findByRoomIdOrderByIdDesc(Long roomId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "music"})
    Slice<RoomPost> findByRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long cursor, Pageable pageable);

    Optional<RoomPost> findByIdAndRoomId(Long id, Long roomId);
}
