package com.tuneet.tuneetbe.domain.like.repository;

import com.tuneet.tuneetbe.domain.like.entity.UserArtistLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserArtistLikeRepository extends JpaRepository<UserArtistLike, Long> {

    // 이미 있으면 그대로 둠 (여러 번 눌러도 결과가 같도록)
    @Modifying
    @Query(value = """
            INSERT INTO user_artist_likes (user_id, artist_id)
            VALUES (:userId, :artistId)
            ON DUPLICATE KEY UPDATE user_artist_like_id = user_artist_like_id
            """, nativeQuery = true)
    void insertIfAbsent(@Param("userId") Long userId, @Param("artistId") Long artistId);

    // 아티스트 MBID로 내 좋아요 ID 조회 (없으면 빈 목록)
    @Query("SELECT l.id FROM UserArtistLike l WHERE l.user.id = :userId AND l.artist.externalId = :externalId")
    List<Long> findIdsByUserIdAndArtistExternalId(@Param("userId") Long userId, @Param("externalId") String externalId);

    // 좋아요한 아티스트 목록 (최근 좋아요한 순)
    @EntityGraph(attributePaths = "artist")
    Slice<UserArtistLike> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "artist")
    Slice<UserArtistLike> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);

    // 사용자의 좋아요한 아티스트 ID
    @Query("SELECT l.artist.id FROM UserArtistLike l WHERE l.user.id = :userId")
    List<Long> findArtistIdsByUserId(@Param("userId") Long userId);

    // 여러 사용자의 좋아요한 아티스트 중 겹치는 아티스트
    @EntityGraph(attributePaths = "artist")
    List<UserArtistLike> findAllByUserIdInAndArtistIdInOrderByIdAsc(Collection<Long> userIds, Collection<Long> artistIds);

    // 좋아요한 아티스트가 있는 사용자 ID
    @Query("SELECT DISTINCT l.user.id FROM UserArtistLike l WHERE l.user.id IN :userIds")
    List<Long> findUserIdsHavingArtistLike(@Param("userIds") Collection<Long> userIds);

    // 사용자의 좋아요한 아티스트 전체 삭제
    void deleteAllByUserId(Long userId);
}
