package com.tuneet.tuneetbe.domain.feed.service;

import com.tuneet.tuneetbe.domain.feed.dto.response.FeedCreateResponse;
import com.tuneet.tuneetbe.domain.feed.dto.response.FeedDetailResponse;
import com.tuneet.tuneetbe.domain.feed.dto.response.FeedListResponse;
import com.tuneet.tuneetbe.domain.feed.dto.response.FeedSendMessageResponse;
import com.tuneet.tuneetbe.domain.feed.dto.response.FeedUpdateResponse;
import com.tuneet.tuneetbe.domain.feed.entity.Feed;
import com.tuneet.tuneetbe.domain.feed.entity.FeedMessage;
import com.tuneet.tuneetbe.domain.feed.exception.FeedErrorCode;
import com.tuneet.tuneetbe.domain.feed.repository.FeedMessageRepository;
import com.tuneet.tuneetbe.domain.feed.repository.FeedRepository;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.notification.entity.Notification;
import com.tuneet.tuneetbe.domain.notification.enums.NotificationType;
import com.tuneet.tuneetbe.domain.notification.repository.NotificationRepository;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long FEED_ID = 100L;
    private static final Long MUSIC_ID = 10L;
    private static final String CONTENT = "이 노래 너무 좋아요!";

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FeedMessageRepository feedMessageRepository;

    @Mock
    private NotificationRepository notificationRepository;

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

    private User writer(Long id, String nickname, String profilePicture) {
        User user = savedUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.updateProfile(nickname, profilePicture);
        return user;
    }

    private Feed savedFeed(Long id, User writer) {
        Feed feed = Feed.builder()
                .user(writer)
                .music(savedMusic())
                .content(CONTENT)
                .build();
        ReflectionTestUtils.setField(feed, "id", id);
        return feed;
    }

    @Nested
    @DisplayName("피드 상세보기")
    class GetFeed {

        @Test
        @DisplayName("내가 작성한 피드면 isMine이 true이고 피드 정보를 반환한다")
        void myFeed() {
            // given
            given(feedRepository.findById(FEED_ID))
                    .willReturn(Optional.of(savedFeed(FEED_ID, writer(USER_ID, "튜니", "profile.png"))));

            // when
            FeedDetailResponse response = feedService.getFeed(USER_ID, FEED_ID);

            // then
            assertThat(response.getFeedId()).isEqualTo(FEED_ID);
            assertThat(response.getNickname()).isEqualTo("튜니");
            assertThat(response.getProfilePicture()).isEqualTo("profile.png");
            assertThat(response.getContent()).isEqualTo(CONTENT);
            assertThat(response.getMusic().getMusicId()).isEqualTo(MUSIC_ID);
            assertThat(response.getIsMine()).isTrue();
        }

        @Test
        @DisplayName("다른 사용자의 피드면 isMine이 false이고 작성자의 정보를 반환한다")
        void otherUserFeed() {
            // given
            given(feedRepository.findById(FEED_ID))
                    .willReturn(Optional.of(savedFeed(FEED_ID, writer(OTHER_USER_ID, "산책하는 튜니", "other.png"))));

            // when
            FeedDetailResponse response = feedService.getFeed(USER_ID, FEED_ID);

            // then
            assertThat(response.getNickname()).isEqualTo("산책하는 튜니");
            assertThat(response.getProfilePicture()).isEqualTo("other.png");
            assertThat(response.getIsMine()).isFalse();
        }

        @Test
        @DisplayName("피드가 없으면 FEED_NOT_FOUND 예외가 발생한다")
        void feedNotFound() {
            // given
            given(feedRepository.findById(FEED_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedService.getFeed(USER_ID, FEED_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("피드 리스트 보기")
    class GetFeeds {

        private static final int SIZE = 2;
        private final PageRequest pageable = PageRequest.of(0, SIZE);

        @Test
        @DisplayName("커서가 없으면 최신 피드부터 조회하고 다음 피드 여부를 반환한다")
        void firstPage() {
            // given
            User me = writer(USER_ID, "튜니", null);
            User other = writer(OTHER_USER_ID, "산책하는 튜니", null);
            given(feedRepository.findAllByOrderByIdDesc(pageable)).willReturn(
                    new SliceImpl<>(List.of(savedFeed(3L, me), savedFeed(2L, other)), pageable, true));

            // when
            FeedListResponse response = feedService.getFeeds(USER_ID, null, SIZE);

            // then
            assertThat(response.getFeeds()).extracting("feedId").containsExactly(3L, 2L);
            assertThat(response.getFeeds()).extracting("isMine").containsExactly(true, false);
            assertThat(response.getFeeds()).extracting("nickname").containsExactly("튜니", "산책하는 튜니");
            assertThat(response.getHasNext()).isTrue();
            verify(feedRepository, never()).findByIdLessThanOrderByIdDesc(any(), any());
        }

        @Test
        @DisplayName("커서가 있으면 커서보다 이전 피드를 조회한다")
        void nextPage() {
            // given
            User me = writer(USER_ID, "튜니", null);
            given(feedRepository.findByIdLessThanOrderByIdDesc(2L, pageable)).willReturn(
                    new SliceImpl<>(List.of(savedFeed(1L, me)), pageable, false));

            // when
            FeedListResponse response = feedService.getFeeds(USER_ID, 2L, SIZE);

            // then
            assertThat(response.getFeeds()).extracting("feedId").containsExactly(1L);
            assertThat(response.getHasNext()).isFalse();
            verify(feedRepository, never()).findAllByOrderByIdDesc(any());
        }

        @Test
        @DisplayName("피드가 없으면 빈 목록과 hasNext false를 반환한다")
        void emptyFeeds() {
            // given
            given(feedRepository.findAllByOrderByIdDesc(pageable)).willReturn(
                    new SliceImpl<>(List.of(), pageable, false));

            // when
            FeedListResponse response = feedService.getFeeds(USER_ID, null, SIZE);

            // then
            assertThat(response.getFeeds()).isEmpty();
            assertThat(response.getHasNext()).isFalse();
        }
    }

    private Music newMusic() {
        Music music = BeanUtils.instantiateClass(Music.class);
        ReflectionTestUtils.setField(music, "id", 20L);
        ReflectionTestUtils.setField(music, "title", "I GOT YOU");
        ReflectionTestUtils.setField(music, "artist", "TWICE");
        ReflectionTestUtils.setField(music, "musicPicture", "new-cover.png");
        return music;
    }

    @Nested
    @DisplayName("피드 수정")
    class UpdateFeed {

        private static final String NEW_CONTENT = "요즘 이 노래만 들어요";

        @Test
        @DisplayName("노래 없이 코멘트만 보내면 코멘트만 수정하고 기존 노래를 유지한다")
        void contentOnly() {
            // given
            User me = writer(USER_ID, "튜니", null);
            Feed feed = savedFeed(FEED_ID, me);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(me));
            given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));

            // when
            FeedUpdateResponse response = feedService.updateFeed(USER_ID, null, NEW_CONTENT, FEED_ID);

            // then
            assertThat(feed.getContent()).isEqualTo(NEW_CONTENT);
            assertThat(feed.getMusic().getId()).isEqualTo(MUSIC_ID);
            assertThat(response.getContent()).isEqualTo(NEW_CONTENT);
            assertThat(response.getMusic().getMusicId()).isEqualTo(MUSIC_ID);
            assertThat(response.getMusic().getTitle()).isEqualTo("Pretty Girl");
        }

        @Test
        @DisplayName("노래와 코멘트를 보내면 둘 다 수정한다")
        void musicAndContent() {
            // given
            User me = writer(USER_ID, "튜니", null);
            Feed feed = savedFeed(FEED_ID, me);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(me));
            given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));

            // when
            FeedUpdateResponse response = feedService.updateFeed(USER_ID, newMusic(), NEW_CONTENT, FEED_ID);

            // then
            assertThat(feed.getMusic().getId()).isEqualTo(20L);
            assertThat(response.getMusic().getMusicId()).isEqualTo(20L);
            assertThat(response.getMusic().getTitle()).isEqualTo("I GOT YOU");
            assertThat(response.getContent()).isEqualTo(NEW_CONTENT);
        }

        @Test
        @DisplayName("다른 사용자의 피드를 수정하면 FEED_ACCESS_DENIED 예외가 발생하고 수정하지 않는다")
        void otherUserFeed() {
            // given
            Feed feed = savedFeed(FEED_ID, writer(OTHER_USER_ID, "산책하는 튜니", null));
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(writer(USER_ID, "튜니", null)));
            given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));

            // when & then
            assertThatThrownBy(() -> feedService.updateFeed(USER_ID, newMusic(), NEW_CONTENT, FEED_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_ACCESS_DENIED);

            assertThat(feed.getContent()).isEqualTo(CONTENT);
            assertThat(feed.getMusic().getId()).isEqualTo(MUSIC_ID);
        }

        @Test
        @DisplayName("피드가 없으면 FEED_NOT_FOUND 예외가 발생한다")
        void feedNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(writer(USER_ID, "튜니", null)));
            given(feedRepository.findById(FEED_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedService.updateFeed(USER_ID, null, NEW_CONTENT, FEED_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("피드 삭제")
    class DeleteFeed {

        @Test
        @DisplayName("피드에 달린 메시지를 먼저 삭제한 뒤 피드를 삭제한다")
        void success() {
            // given
            User me = writer(USER_ID, "튜니", null);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(me));
            given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(savedFeed(FEED_ID, me)));

            // when
            feedService.deleteFeed(USER_ID, FEED_ID);

            // then
            InOrder inOrder = inOrder(feedMessageRepository, feedRepository);
            inOrder.verify(feedMessageRepository).deleteAllByFeedId(FEED_ID);
            inOrder.verify(feedRepository).deleteById(FEED_ID);
        }

        @Test
        @DisplayName("다른 사용자의 피드를 삭제하면 FEED_ACCESS_DENIED 예외가 발생하고 삭제하지 않는다")
        void otherUserFeed() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(writer(USER_ID, "튜니", null)));
            given(feedRepository.findById(FEED_ID))
                    .willReturn(Optional.of(savedFeed(FEED_ID, writer(OTHER_USER_ID, "산책하는 튜니", null))));

            // when & then
            assertThatThrownBy(() -> feedService.deleteFeed(USER_ID, FEED_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_ACCESS_DENIED);

            verify(feedMessageRepository, never()).deleteAllByFeedId(any());
            verify(feedRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("피드가 없으면 FEED_NOT_FOUND 예외가 발생한다")
        void feedNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(writer(USER_ID, "튜니", null)));
            given(feedRepository.findById(FEED_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedService.deleteFeed(USER_ID, FEED_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_NOT_FOUND);

            verify(feedRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("피드 메시지 작성")
    class SendMessage {

        private static final Long MESSAGE_ID = 500L;
        private static final String MESSAGE = "이 노래도 좋아하실 것 같아요!";

        @Test
        @DisplayName("메시지를 저장하고 피드 작성자에게 알림을 저장한다")
        void success() {
            // given
            User me = writer(USER_ID, "튜니", "me.png");
            User feedWriter = writer(OTHER_USER_ID, "산책하는 튜니", null);
            Feed feed = savedFeed(FEED_ID, feedWriter);
            Music music = newMusic();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(me));
            given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));
            given(feedMessageRepository.save(any(FeedMessage.class))).willAnswer(invocation -> {
                FeedMessage feedMessage = invocation.getArgument(0);
                ReflectionTestUtils.setField(feedMessage, "id", MESSAGE_ID);
                return feedMessage;
            });

            // when
            FeedSendMessageResponse response = feedService.sendMessage(USER_ID, FEED_ID, MESSAGE, music);

            // then
            ArgumentCaptor<FeedMessage> messageCaptor = ArgumentCaptor.forClass(FeedMessage.class);
            verify(feedMessageRepository).save(messageCaptor.capture());
            assertThat(messageCaptor.getValue().getUser()).isEqualTo(me);
            assertThat(messageCaptor.getValue().getFeed()).isEqualTo(feed);
            assertThat(messageCaptor.getValue().getMusic()).isEqualTo(music);
            assertThat(messageCaptor.getValue().getMessageContent()).isEqualTo(MESSAGE);

            ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification notification = notificationCaptor.getValue();
            assertThat(notification.getReceiver()).isEqualTo(feedWriter);
            assertThat(notification.getSender()).isEqualTo(me);
            assertThat(notification.getType()).isEqualTo(NotificationType.FEED_MESSAGE);
            assertThat(notification.getTargetId()).isEqualTo(MESSAGE_ID);
            assertThat(notification.getIsRead()).isFalse();

            assertThat(response.getFeedMessageId()).isEqualTo(MESSAGE_ID);
            assertThat(response.getFeedId()).isEqualTo(FEED_ID);
            assertThat(response.getNickname()).isEqualTo("튜니");
            assertThat(response.getProfilePicture()).isEqualTo("me.png");
            assertThat(response.getMusic().getMusicId()).isEqualTo(20L);
            assertThat(response.getMessageContent()).isEqualTo(MESSAGE);
        }

        @Test
        @DisplayName("내 피드에 메시지를 보내면 FEED_MESSAGE_SELF 예외가 발생하고 저장하지 않는다")
        void selfFeed() {
            // given
            User me = writer(USER_ID, "튜니", null);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(me));
            given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(savedFeed(FEED_ID, me)));

            // when & then
            assertThatThrownBy(() -> feedService.sendMessage(USER_ID, FEED_ID, MESSAGE, newMusic()))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_MESSAGE_SELF);

            verify(feedMessageRepository, never()).save(any());
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("피드가 없으면 FEED_NOT_FOUND 예외가 발생하고 저장하지 않는다")
        void feedNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(writer(USER_ID, "튜니", null)));
            given(feedRepository.findById(FEED_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedService.sendMessage(USER_ID, FEED_ID, MESSAGE, newMusic()))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_NOT_FOUND);

            verify(feedMessageRepository, never()).save(any());
            verify(notificationRepository, never()).save(any());
        }
    }
}
