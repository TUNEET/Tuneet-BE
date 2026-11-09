package com.tuneet.tuneetbe.domain.music.repository;

import com.tuneet.tuneetbe.domain.music.entity.Music;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface MusicRepository extends JpaRepository<Music, Long> {

    List<Music> findAllByExternalIdIn(Collection<String> externalIds);

    // 동시에 같은 곡을 저장해도 충돌하지 않도록 없을 때만 저장
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO music (title, artist, music_picture, external_id, canonical_id, artist_id)
            VALUES (:title, :artist, :musicPicture, :externalId, :canonicalId, :artistId)
            ON DUPLICATE KEY UPDATE music_id = music_id
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("title") String title,
            @Param("artist") String artist,
            @Param("musicPicture") String musicPicture,
            @Param("externalId") String externalId,
            @Param("canonicalId") String canonicalId,
            @Param("artistId") Long artistId);
}
