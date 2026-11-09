package com.tuneet.tuneetbe.domain.feed.service;

import com.tuneet.tuneetbe.domain.feed.dto.response.FeedCreateResponse;
import com.tuneet.tuneetbe.domain.feed.entity.Feed;
import com.tuneet.tuneetbe.domain.feed.repository.FeedRepository;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long FEED_ID = 100L;
    private static final Long MUSIC_ID = 10L;
    private static final String CONTENT = "이 노래 너무 좋아요!";

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FeedService feedService;

    private User savedUser() {
        User user = User.builder()
                .email("test01@email.com")
                .password("encoded-password")
                .termsAgreedAt(LocalDateTime.now())
                .privacyAgreedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.updateProfile("튜니", null);
        return user;
    }

    private Music savedMusic() {
        Music music = BeanUtils.instantiateClass(Music.class);
        ReflectionTestUtils.setField(music, "id", MUSIC_ID);
        ReflectionTestUtils.setField(music, "title", "Pretty Girl");
        ReflectionTestUtils.setField(music, "artist", "RESCENE");
        ReflectionTestUtils.setField(music, "musicPicture", "cover.png");
        return music;
    }

    @Nested
    @DisplayName("피드 작성")
    class CreateFeed {

        @Test
        @DisplayName("작성자, 노래, 코멘트를 저장하고 응답을 반환한다")
        void success() {
            // given
            User user = savedUser();
            Music music = savedMusic();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(feedRepository.save(any(Feed.class))).willAnswer(invocation -> {
                Feed feed = invocation.getArgument(0);
                ReflectionTestUtils.setField(feed, "id", FEED_ID);
                return feed;
            });

            // when
            FeedCreateResponse response = feedService.createFeed(USER_ID, music, CONTENT);

            // then
            ArgumentCaptor<Feed> captor = ArgumentCaptor.forClass(Feed.class);
            verify(feedRepository).save(captor.capture());
            Feed savedFeed = captor.getValue();

            assertThat(savedFeed.getUser()).isEqualTo(user);
            assertThat(savedFeed.getMusic()).isEqualTo(music);
            assertThat(savedFeed.getContent()).isEqualTo(CONTENT);

            assertThat(response.getFeedId()).isEqualTo(FEED_ID);
            assertThat(response.getNickname()).isEqualTo("튜니");
            assertThat(response.getContent()).isEqualTo(CONTENT);
            assertThat(response.getMusic().getMusicId()).isEqualTo(MUSIC_ID);
            assertThat(response.getMusic().getTitle()).isEqualTo("Pretty Girl");
            assertThat(response.getMusic().getArtist()).isEqualTo("RESCENE");
            assertThat(response.getMusic().getMusicPicture()).isEqualTo("cover.png");
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생하고 저장하지 않는다")
        void userNotFound() {
            // given
            Music music = savedMusic();
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedService.createFeed(USER_ID, music, CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);

            verify(feedRepository, never()).save(any(Feed.class));
        }
    }
}
