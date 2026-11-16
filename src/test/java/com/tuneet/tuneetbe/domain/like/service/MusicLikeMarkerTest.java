package com.tuneet.tuneetbe.domain.like.service;

import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.user.repository.UserMusicLikeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MusicLikeMarkerTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserMusicLikeRepository userMusicLikeRepository;

    @InjectMocks
    private MusicLikeMarker marker;

    private MusicResponse music(String id) {
        return MusicResponse.builder().musicId(id).title("제목").build();
    }

    @Test
    @DisplayName("좋아요한 곡은 liked=true, 아닌 곡은 false로 한 번의 조회로 표시한다 (같은 곡이 여러 번 나와도)")
    void mark() {
        MusicResponse liked = music("rec-a");
        MusicResponse sameAgain = music("rec-a");
        MusicResponse notLiked = music("rec-b");
        given(userMusicLikeRepository.findLikedExternalIds(USER_ID, Set.of("rec-a", "rec-b"))).willReturn(List.of("rec-a"));

        marker.mark(USER_ID, Arrays.asList(liked, null, sameAgain, notLiked));

        assertThat(liked.getLiked()).isTrue();
        assertThat(sameAgain.getLiked()).isTrue();
        assertThat(notLiked.getLiked()).isFalse();
    }

    @Test
    @DisplayName("곡이 없으면 조회하지 않는다")
    void empty() {
        marker.mark(USER_ID, Arrays.asList((MusicResponse) null));

        verify(userMusicLikeRepository, never()).findLikedExternalIds(any(), any());
    }
}
