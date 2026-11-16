package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserMusic;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserMusicRepository extends JpaRepository<UserMusic, Long> {

    List<UserMusic> findAllByUserId(Long userId);
    // 사용자의 좋아하는 노래의 대표 곡 ID
    @Query("SELECT DISTINCT um.music.canonicalId FROM UserMusic um WHERE um.user.id = :userId")
    List<String> findCanonicalIdsByUserId(@Param("userId") Long userId);

    // 여러 사용자의 좋아하는 노래 중 대표 곡 ID가 같은 노래
    @EntityGraph(attributePaths = "music")
    List<UserMusic> findAllByUserIdInAndMusicCanonicalIdInOrderByIdAsc(Collection<Long> userIds, Collection<String> canonicalIds);

    // 좋아하는 노래가 있는 사용자 ID
    @Query("SELECT DISTINCT um.user.id FROM UserMusic um WHERE um.user.id IN :userIds")
    List<Long> findUserIdsHavingMusic(@Param("userIds") Collection<Long> userIds);

    // 사용자의 좋아하는 노래 전체 삭제
    void deleteAllByUserId(Long userId);
}
