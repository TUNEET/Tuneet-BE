package com.tuneet.tuneetbe.domain.music.service;

import com.tuneet.tuneetbe.domain.music.client.MusicApiClient;
import com.tuneet.tuneetbe.domain.music.dto.response.ArtistSearchItemResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicBatchResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicSearchItemResponse;
import com.tuneet.tuneetbe.domain.music.entity.Artist;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.exception.MusicErrorCode;
import com.tuneet.tuneetbe.domain.music.repository.ArtistRepository;
import com.tuneet.tuneetbe.domain.music.repository.MusicRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 음악 API의 곡·아티스트를 스프링 DB에 저장한다.
 * 외부 호출 동안 DB 트랜잭션을 잡지 않도록 메서드에 트랜잭션을 걸지 않는다.
 */
@Service
@RequiredArgsConstructor
public class MusicCatalogService {

    private final MusicApiClient musicApiClient;
    private final MusicRepository musicRepository;
    private final ArtistRepository artistRepository;

    // 요청한 순서대로 곡 조회, 없는 곡은 음악 API에서 가져와 저장
    public List<Music> getOrCreateMusics(Collection<String> externalIds) {
        List<String> missing = missingFrom(findMusics(externalIds), externalIds);
        if (!missing.isEmpty()) {
            MusicBatchResponse batch = musicApiClient.getMusics(missing);
            if (!batch.missingIds().isEmpty()) {
                throw new CustomException(MusicErrorCode.MUSIC_NOT_FOUND);
            }
            saveMusics(batch.items());
        }
        Map<String, Music> musics = findMusics(externalIds);
        return externalIds.stream().map(musics::get).toList();
    }

    // 요청한 순서대로 아티스트 조회, 없는 아티스트는 음악 API에서 가져와 저장
    public List<Artist> getOrCreateArtists(Collection<String> externalIds) {
        for (String externalId : missingFrom(findArtists(externalIds), externalIds)) {
            ArtistSearchItemResponse artist = musicApiClient.getArtist(externalId)
                    .orElseThrow(() -> new CustomException(MusicErrorCode.ARTIST_NOT_FOUND));
            artistRepository.insertIfAbsent(artist.name(), artist.id());
        }
        Map<String, Artist> artists = findArtists(externalIds);
        return externalIds.stream().map(artists::get).toList();
    }

    private void saveMusics(List<MusicSearchItemResponse> items) {
        Map<String, Long> artistIds = savePrimaryArtists(items);
        for (MusicSearchItemResponse item : items) {
            Long artistId = item.artists().isEmpty() ? null : artistIds.get(item.artists().get(0).id());
            musicRepository.insertIfAbsent(
                    item.title(), item.artist(), item.coverUrl(), item.id(), item.canonicalId(), artistId);
        }
    }

    // 곡의 첫 번째 아티스트를 대표 아티스트로 저장
    private Map<String, Long> savePrimaryArtists(List<MusicSearchItemResponse> items) {
        Map<String, String> names = new LinkedHashMap<>();
        for (MusicSearchItemResponse item : items) {
            if (!item.artists().isEmpty()) {
                names.put(item.artists().get(0).id(), item.artists().get(0).name());
            }
        }
        if (names.isEmpty()) {
            return Map.of();
        }
        names.forEach((externalId, name) -> artistRepository.insertIfAbsent(name, externalId));
        return artistRepository.findAllByExternalIdIn(names.keySet()).stream()
                .collect(Collectors.toMap(Artist::getExternalId, Artist::getId));
    }

    private Map<String, Music> findMusics(Collection<String> externalIds) {
        return musicRepository.findAllByExternalIdIn(externalIds).stream()
                .collect(Collectors.toMap(Music::getExternalId, Function.identity()));
    }

    private Map<String, Artist> findArtists(Collection<String> externalIds) {
        return artistRepository.findAllByExternalIdIn(externalIds).stream()
                .collect(Collectors.toMap(Artist::getExternalId, Function.identity()));
    }

    private List<String> missingFrom(Map<String, ?> found, Collection<String> externalIds) {
        return externalIds.stream().filter(id -> !found.containsKey(id)).toList();
    }
}
