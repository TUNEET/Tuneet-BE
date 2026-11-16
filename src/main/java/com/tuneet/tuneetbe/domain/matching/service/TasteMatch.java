package com.tuneet.tuneetbe.domain.matching.service;

import com.tuneet.tuneetbe.domain.music.entity.Artist;
import com.tuneet.tuneetbe.domain.music.entity.Genre;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.user.enums.DistanceRange;

import java.util.List;

/**
 * 다른 사용자 한 명과의 매칭 결과
 * matchPercent: 내가 매칭을 쓸 수 없거나(위치 미동의, 위치 노출 OFF) 한쪽의 취향 데이터가 없으면 null
 * distanceRange: 나 또는 상대가 위치 미동의, 위치 노출 OFF이거나 좌표가 없으면 null, 차단 관계(양방향)여도 null
 * commonMusics: 같은 곡(대표 곡 ID 기준)은 한 번씩, 상대의 좋아하는 노래 다음 좋아요한 노래 순
 * commonArtists: 같은 아티스트는 한 번씩, 상대의 선호 아티스트 다음 좋아요한 아티스트 순
 */
public record TasteMatch(
        Integer matchPercent,
        DistanceRange distanceRange,
        List<Music> commonMusics,
        List<Artist> commonArtists,
        List<Genre> commonGenres) {
}
