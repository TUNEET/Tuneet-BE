package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserMusicLike;
import com.tuneet.tuneetbe.domain.user.enums.LikeSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserMusicLikeRepository extends JpaRepository<UserMusicLike, Long> {

    // 좋아요한 노래의 대표 곡 ID (출처 상관없이)
    @Query("SELECT DISTINCT l.music.canonicalId FROM UserMusicLike l WHERE l.user.id = :userId")
    List<String> findCanonicalIdsByUserId(@Param("userId") Long userId);

    // 여러 사용자의 좋아요한 노래 중 대표 곡 ID가 같은 노래
    @EntityGraph(attributePaths = "music")
    List<UserMusicLike> findAllByUserIdInAndMusicCanonicalIdInOrderByIdAsc(Collection<Long> userIds, Collection<String> canonicalIds);

    // 좋아요한 노래가 있는 사용자 ID
    @Query("SELECT DISTINCT l.user.id FROM UserMusicLike l WHERE l.user.id IN :userIds")
    List<Long> findUserIdsHavingLike(@Param("userIds") Collection<Long> userIds);

    // 곡 MBID 중 내가 좋아요한 곡 (같은 곡의 다른 음원, 불러온 좋아요 포함)
    @Query("""
            SELECT m.externalId FROM Music m
            WHERE m.externalId IN :externalIds
              AND m.canonicalId IN (SELECT l.music.canonicalId FROM UserMusicLike l WHERE l.user.id = :userId)
            """)
    List<String> findLikedExternalIds(@Param("userId") Long userId, @Param("externalIds") Collection<String> externalIds);

    // 곡 MBID와 같은 곡인 내 좋아요 ID (출처 상관없이)
    @Query("""
            SELECT l.id FROM UserMusicLike l
            WHERE l.user.id = :userId
              AND l.music.canonicalId IN (SELECT m.canonicalId FROM Music m WHERE m.externalId = :externalId)
            """)
    List<Long> findIdsByUserIdAndSameSongAs(@Param("userId") Long userId, @Param("externalId") String externalId);

    // 좋아요한 곡 목록: 같은 곡은 가장 최근 좋아요 하나만, 최근 좋아요한 순
    @Query("""
            SELECT l FROM UserMusicLike l JOIN FETCH l.music m
            WHERE l.user.id = :userId
              AND (:cursor IS NULL OR l.id < :cursor)
              AND l.id = (SELECT MAX(l2.id) FROM UserMusicLike l2
                          WHERE l2.user.id = :userId AND l2.music.canonicalId = m.canonicalId)
            ORDER BY l.id DESC
            """)
    Slice<UserMusicLike> findLatestPerSong(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT l.music.id FROM UserMusicLike l WHERE l.user.id = :userId AND l.source = :source")
    List<Long> findMusicIdsByUserIdAndSource(@Param("userId") Long userId, @Param("source") LikeSource source);

    // 이미 있으면 그대로 둠
    @Modifying
    @Query(value = """
            INSERT INTO user_music_likes (user_id, music_id, source)
            VALUES (:userId, :musicId, :source)
            ON DUPLICATE KEY UPDATE user_music_like_id = user_music_like_id
            """, nativeQuery = true)
    void insertIfAbsent(@Param("userId") Long userId, @Param("musicId") Long musicId, @Param("source") String source);

    @Modifying
    @Query("DELETE FROM UserMusicLike l WHERE l.user.id = :userId AND l.source = :source AND l.music.id IN :musicIds")
    void deleteByUserIdAndSourceAndMusicIdIn(
            @Param("userId") Long userId, @Param("source") LikeSource source, @Param("musicIds") Collection<Long> musicIds);

    @Modifying
    @Query("DELETE FROM UserMusicLike l WHERE l.user.id = :userId AND l.source = :source")
    void deleteAllByUserIdAndSource(@Param("userId") Long userId, @Param("source") LikeSource source);

    // 회원탈퇴 시 전체 삭제
    @Modifying
    @Query("DELETE FROM UserMusicLike l WHERE l.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
