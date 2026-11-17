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

    // 동시에 같은 아티스트를 저장해도 충돌하지 않도록 없을 때만 저장, 이미 있으면 비어 있는 이미지만 채움
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO artists (name, artist_image, external_id)
            VALUES (:name, :artistImage, :externalId)
            ON DUPLICATE KEY UPDATE artist_image = COALESCE(artist_image, :artistImage)
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("name") String name,
            @Param("artistImage") String artistImage,
            @Param("externalId") String externalId);
}
