package com.tuneet.tuneetbe.domain.music.repository;

import com.tuneet.tuneetbe.domain.music.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    List<Artist> findAllByExternalIdIn(Collection<String> externalIds);

    // 동시에 같은 아티스트를 저장해도 충돌하지 않도록 없을 때만 저장
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO artists (name, external_id)
            VALUES (:name, :externalId)
            ON DUPLICATE KEY UPDATE artist_id = artist_id
            """, nativeQuery = true)
    void insertIfAbsent(@Param("name") String name, @Param("externalId") String externalId);
}
