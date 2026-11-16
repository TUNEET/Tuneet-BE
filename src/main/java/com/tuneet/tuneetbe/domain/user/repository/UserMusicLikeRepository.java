package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserMusicLike;
import com.tuneet.tuneetbe.domain.user.enums.LikeSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserMusicLikeRepository extends JpaRepository<UserMusicLike, Long> {

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
