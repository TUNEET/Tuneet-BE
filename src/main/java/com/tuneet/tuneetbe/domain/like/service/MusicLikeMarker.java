package com.tuneet.tuneetbe.domain.like.service;

import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.user.repository.UserMusicLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 곡 응답에 내가 좋아요한 곡인지 표시한다. (목록 하나에 쿼리 1번)
 * 같은 곡의 다른 음원, 유튜브에서 불러온 좋아요도 좋아요한 곡으로 본다.
 */
@Component
@RequiredArgsConstructor
public class MusicLikeMarker {

    private final UserMusicLikeRepository userMusicLikeRepository;

    public void mark(Long userId, Collection<MusicResponse> musics) {
        List<MusicResponse> targets = musics.stream().filter(Objects::nonNull).toList();
        Set<String> externalIds = new HashSet<>();
        targets.forEach(music -> {
            if (music.getMusicId() != null) {
                externalIds.add(music.getMusicId());
            }
        });
        if (externalIds.isEmpty()) {
            return;
        }

        Set<String> liked = new HashSet<>(userMusicLikeRepository.findLikedExternalIds(userId, externalIds));
        targets.forEach(music -> music.markLiked(liked.contains(music.getMusicId())));
    }
}
