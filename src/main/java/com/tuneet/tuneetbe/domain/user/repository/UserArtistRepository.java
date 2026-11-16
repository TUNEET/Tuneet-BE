package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserArtist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserArtistRepository extends JpaRepository<UserArtist, Long> {

    List<UserArtist> findAllByUserId(Long userId);

    // 사용자의 좋아하는 아티스트를 아티스트 정보와 함께 조회 (저장한 순서)
    @EntityGraph(attributePaths = "artist")
    List<UserArtist> findAllWithArtistByUserIdOrderByIdAsc(Long userId);

    // 사용자의 좋아하는 아티스트 ID
    @Query("SELECT ua.artist.id FROM UserArtist ua WHERE ua.user.id = :userId")
    List<Long> findArtistIdsByUserId(@Param("userId") Long userId);

    // 여러 사용자의 좋아하는 아티스트 중 겹치는 아티스트
    @EntityGraph(attributePaths = "artist")
    List<UserArtist> findAllByUserIdInAndArtistIdInOrderByIdAsc(Collection<Long> userIds, Collection<Long> artistIds);

    // 좋아하는 아티스트가 있는 사용자 ID
    @Query("SELECT DISTINCT ua.user.id FROM UserArtist ua WHERE ua.user.id IN :userIds")
    List<Long> findUserIdsHavingArtist(@Param("userIds") Collection<Long> userIds);

    // 사용자의 좋아하는 아티스트 전체 삭제
    void deleteAllByUserId(Long userId);
}
