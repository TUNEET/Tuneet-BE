package com.tuneet.tuneetbe.domain.music.repository;

import com.tuneet.tuneetbe.domain.music.entity.YoutubeVideoMatch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface YoutubeVideoMatchRepository extends JpaRepository<YoutubeVideoMatch, String> {

    // 곡과 대표 아티스트까지 함께 조회 (트랜잭션 밖에서 쓰므로 지연 로딩 없이)
    @EntityGraph(attributePaths = {"music", "music.primaryArtist"})
    List<YoutubeVideoMatch> findAllByVideoIdIn(Collection<String> videoIds);

    // 다른 사용자가 같은 영상을 동시에 매칭해도 충돌하지 않도록 저장 또는 갱신
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO youtube_video_matches (video_id, music_id, is_cover, matched_at)
            VALUES (:videoId, :musicId, :isCover, :matchedAt) AS new
            ON DUPLICATE KEY UPDATE music_id = new.music_id, is_cover = new.is_cover, matched_at = new.matched_at
            """, nativeQuery = true)
    void upsert(
            @Param("videoId") String videoId,
            @Param("musicId") Long musicId,
            @Param("isCover") boolean isCover,
            @Param("matchedAt") LocalDateTime matchedAt);
}
