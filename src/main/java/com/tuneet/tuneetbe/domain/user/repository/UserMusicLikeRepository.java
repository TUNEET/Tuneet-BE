package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserMusicLike;
import com.tuneet.tuneetbe.domain.user.enums.LikeSource;
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
