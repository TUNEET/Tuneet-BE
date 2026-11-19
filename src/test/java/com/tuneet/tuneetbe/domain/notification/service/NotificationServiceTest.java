package com.tuneet.tuneetbe.domain.notification.service;

import com.tuneet.tuneetbe.domain.like.service.MusicLikeMarker;
import com.tuneet.tuneetbe.domain.feed.entity.Feed;
import com.tuneet.tuneetbe.domain.feed.entity.FeedMessage;
import com.tuneet.tuneetbe.domain.feed.entity.FeedMessageReply;
import com.tuneet.tuneetbe.domain.feed.repository.FeedMessageReplyRepository;
import com.tuneet.tuneetbe.domain.feed.repository.FeedMessageRepository;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.notification.dto.response.NotificationListResponse;
import com.tuneet.tuneetbe.domain.notification.dto.response.NotificationResponse;
import com.tuneet.tuneetbe.domain.notification.entity.Notification;
import com.tuneet.tuneetbe.domain.notification.enums.NotificationType;
import com.tuneet.tuneetbe.domain.notification.repository.NotificationRepository;
import com.tuneet.tuneetbe.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long RECEIVER_ID = 1L;
    private static final Long SENDER_ID = 2L;
    private static final Long FEED_ID = 30L;
    private static final int SIZE = 2;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private FeedMessageRepository feedMessageRepository;

    @Mock
    private MusicLikeMarker musicLikeMarker;

    @Mock
    private FeedMessageReplyRepository feedMessageReplyRepository;

    @InjectMocks
    private NotificationService notificationService;

    private final PageRequest pageable = PageRequest.of(0, SIZE);

    private User savedUser(Long id, String nickname) {
        User user = User.builder()
                .email(nickname + "@email.com")
                .password("encoded-password")
                .termsAgreedAt(LocalDateTime.now())
                .privacyAgreedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        user.updateProfile(nickname, null);
        return user;
    }

    private Music savedMusic() {
        Music music = BeanUtils.instantiateClass(Music.class);
        ReflectionTestUtils.setField(music, "id", 10L);
        ReflectionTestUtils.setField(music, "title", "REBEL HEART");
        ReflectionTestUtils.setField(music, "artist", "IVE");
        ReflectionTestUtils.setField(music, "musicPicture", "cover.png");
        return music;
    }

    private FeedMessage savedMessage(Long id, User sender, String content) {
        Feed feed = Feed.builder().user(savedUser(RECEIVER_ID, "튜니")).music(savedMusic()).content("피드").build();
        ReflectionTestUtils.setField(feed, "id", FEED_ID);
        FeedMessage message = FeedMessage.builder().user(sender).feed(feed).music(savedMusic()).messageContent(content).build();
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    private Notification savedNotification(Long id, User sender, Long targetId) {
        Notification notification = Notification.builder()
                .receiver(savedUser(RECEIVER_ID, "튜니"))
                .sender(sender)
                .type(NotificationType.FEED_MESSAGE)
                .targetId(targetId)
                .build();
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    private FeedMessageReply savedReply(Long id, FeedMessage feedMessage, User sender, String content) {
        FeedMessageReply reply = FeedMessageReply.builder()
                .feedMessage(feedMessage)
                .sender(sender)
                .receiver(savedUser(RECEIVER_ID, "튜니"))
                .music(savedMusic())
                .replyContent(content)
                .build();
        ReflectionTestUtils.setField(reply, "id", id);
        return reply;
    }

    private Notification savedReplyNotification(Long id, User sender, Long replyId) {
        Notification notification = Notification.builder()
                .receiver(savedUser(RECEIVER_ID, "튜니"))
                .sender(sender)
                .type(NotificationType.FEED_MESSAGE_REPLY)
                .targetId(replyId)
                .build();
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    @Nested
    @DisplayName("알림 목록 조회")
    class GetNotifications {

        @Test
        @DisplayName("커서가 없으면 최신 알림부터 조회하고 메시지 내용과 노래를 함께 반환한다")
        void firstPage() {
            // given
            User sender = savedUser(SENDER_ID, "배고픈 튜니");
            given(notificationRepository.findByReceiverIdOrderByIdDesc(RECEIVER_ID, pageable))
                    .willReturn(new SliceImpl<>(List.of(savedNotification(6L, sender, 501L), savedNotification(5L, sender, 500L)), pageable, true));
            given(feedMessageRepository.findAllByIdIn(List.of(501L, 500L)))
                    .willReturn(List.of(savedMessage(500L, sender, "첫 메시지"), savedMessage(501L, sender, "두 번째 메시지")));

            // when
            NotificationListResponse response = notificationService.getNotifications(RECEIVER_ID, null, SIZE);

            // then
            assertThat(response.getNotifications()).extracting(NotificationResponse::getNotificationId).containsExactly(6L, 5L);
            assertThat(response.getNotifications()).extracting(NotificationResponse::getMessageContent)
                    .containsExactly("두 번째 메시지", "첫 메시지");
            NotificationResponse first = response.getNotifications().get(0);
            assertThat(first.getSenderNickname()).isEqualTo("배고픈 튜니");
            assertThat(first.getFeedId()).isEqualTo(FEED_ID);
            assertThat(first.getMusic().getTitle()).isEqualTo("REBEL HEART");
            assertThat(first.getIsRead()).isFalse();
            assertThat(first.getType()).isEqualTo(NotificationType.FEED_MESSAGE);
            assertThat(response.getHasNext()).isTrue();
            verify(notificationRepository, never()).findByReceiverIdAndIdLessThanOrderByIdDesc(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("커서가 있으면 커서보다 이전 알림을 조회한다")
        void nextPage() {
            // given
            User sender = savedUser(SENDER_ID, "배고픈 튜니");
            given(notificationRepository.findByReceiverIdAndIdLessThanOrderByIdDesc(RECEIVER_ID, 5L, pageable))
                    .willReturn(new SliceImpl<>(List.of(savedNotification(4L, sender, 499L)), pageable, false));
            given(feedMessageRepository.findAllByIdIn(List.of(499L))).willReturn(List.of(savedMessage(499L, sender, "메시지")));

            // when
            NotificationListResponse response = notificationService.getNotifications(RECEIVER_ID, 5L, SIZE);

            // then
            assertThat(response.getNotifications()).extracting(NotificationResponse::getNotificationId).containsExactly(4L);
            assertThat(response.getHasNext()).isFalse();
            verify(notificationRepository, never()).findByReceiverIdOrderByIdDesc(anyLong(), any());
        }

        @Test
        @DisplayName("탈퇴한 사용자가 보낸 알림은 보낸 사람을 탈퇴한 사용자로 표시한다")
        void deletedSender() {
            // given
            User sender = savedUser(SENDER_ID, "배고픈 튜니");
            sender.deleteUser(LocalDateTime.now());
            given(notificationRepository.findByReceiverIdOrderByIdDesc(RECEIVER_ID, pageable))
                    .willReturn(new SliceImpl<>(List.of(savedNotification(6L, sender, 501L)), pageable, false));
            given(feedMessageRepository.findAllByIdIn(List.of(501L))).willReturn(List.of(savedMessage(501L, sender, "메시지")));

            // when
            NotificationListResponse response = notificationService.getNotifications(RECEIVER_ID, null, SIZE);

            // then
            assertThat(response.getNotifications().get(0).getSenderNickname()).isEqualTo("탈퇴한 사용자");
        }

        @Test
        @DisplayName("대상 메시지가 없는 알림은 목록에서 제외한다")
        void skipMissingMessage() {
            // given
            User sender = savedUser(SENDER_ID, "배고픈 튜니");
            given(notificationRepository.findByReceiverIdOrderByIdDesc(RECEIVER_ID, pageable))
                    .willReturn(new SliceImpl<>(List.of(savedNotification(6L, sender, 501L), savedNotification(5L, sender, 500L)), pageable, false));
            given(feedMessageRepository.findAllByIdIn(List.of(501L, 500L))).willReturn(List.of(savedMessage(500L, sender, "남은 메시지")));

            // when
            NotificationListResponse response = notificationService.getNotifications(RECEIVER_ID, null, SIZE);

            // then
            assertThat(response.getNotifications()).extracting(NotificationResponse::getNotificationId).containsExactly(5L);
        }

        @Test
        @DisplayName("알림이 없으면 메시지를 조회하지 않고 빈 목록을 반환한다")
        void empty() {
            // given
            given(notificationRepository.findByReceiverIdOrderByIdDesc(RECEIVER_ID, pageable))
                    .willReturn(new SliceImpl<>(List.of(), pageable, false));

            // when
            NotificationListResponse response = notificationService.getNotifications(RECEIVER_ID, null, SIZE);

            // then
            assertThat(response.getNotifications()).isEmpty();
            assertThat(response.getHasNext()).isFalse();
            verify(feedMessageRepository, never()).findAllByIdIn(any());
            verify(feedMessageReplyRepository, never()).findLatestReplyIds(any());
        }

        @Test
        @DisplayName("코멘트 알림은 코멘트 고유번호와 답장 여부를 함께 반환한다")
        void messageReplied() {
            // given
            User sender = savedUser(SENDER_ID, "배고픈 튜니");
            given(notificationRepository.findByReceiverIdOrderByIdDesc(RECEIVER_ID, pageable))
                    .willReturn(new SliceImpl<>(List.of(savedNotification(6L, sender, 501L), savedNotification(5L, sender, 500L)), pageable, false));
            given(feedMessageRepository.findAllByIdIn(List.of(501L, 500L)))
                    .willReturn(List.of(savedMessage(500L, sender, "첫 메시지"), savedMessage(501L, sender, "두 번째 메시지")));
            given(feedMessageReplyRepository.findLatestReplyIds(any())).willReturn(List.<Object[]>of(new Object[]{500L, 900L}));

            // when
            NotificationListResponse response = notificationService.getNotifications(RECEIVER_ID, null, SIZE);

            // then
            assertThat(response.getNotifications()).extracting(NotificationResponse::getFeedMessageId).containsExactly(501L, 500L);
            assertThat(response.getNotifications()).extracting(NotificationResponse::getReplyId).containsOnlyNulls();
            assertThat(response.getNotifications()).extracting(NotificationResponse::getIsReplied).containsExactly(false, true);
        }

        @Test
        @DisplayName("답장 알림은 답장 내용과 노래를 보여주고, 같은 코멘트에 더 나중 답장이 있으면 답장한 것으로 표시한다")
        void replyNotifications() {
            // given
            User sender = savedUser(SENDER_ID, "노래하는 튜니");
            FeedMessage message = savedMessage(500L, savedUser(RECEIVER_ID, "튜니"), "코멘트");
            given(notificationRepository.findByReceiverIdOrderByIdDesc(RECEIVER_ID, pageable))
                    .willReturn(new SliceImpl<>(List.of(savedReplyNotification(8L, sender, 903L), savedReplyNotification(7L, sender, 901L)), pageable, false));
            given(feedMessageReplyRepository.findAllByIdIn(List.of(903L, 901L)))
                    .willReturn(List.of(savedReply(901L, message, sender, "첫 답장"), savedReply(903L, message, sender, "두 번째 답장")));
            given(feedMessageReplyRepository.findLatestReplyIds(List.of(500L))).willReturn(List.<Object[]>of(new Object[]{500L, 903L}));

            // when
            NotificationListResponse response = notificationService.getNotifications(RECEIVER_ID, null, SIZE);

            // then
            assertThat(response.getNotifications()).extracting(NotificationResponse::getReplyId).containsExactly(903L, 901L);
            assertThat(response.getNotifications()).extracting(NotificationResponse::getMessageContent).containsExactly("두 번째 답장", "첫 답장");
            assertThat(response.getNotifications()).extracting(NotificationResponse::getIsReplied).containsExactly(false, true);
            NotificationResponse first = response.getNotifications().get(0);
            assertThat(first.getType()).isEqualTo(NotificationType.FEED_MESSAGE_REPLY);
            assertThat(first.getFeedMessageId()).isEqualTo(500L);
            assertThat(first.getFeedId()).isEqualTo(FEED_ID);
            assertThat(first.getSenderNickname()).isEqualTo("노래하는 튜니");
            assertThat(first.getMusic().getTitle()).isEqualTo("REBEL HEART");
            verify(feedMessageRepository, never()).findAllByIdIn(any());
        }

        @Test
        @DisplayName("대상 답장이 없는 답장 알림은 목록에서 제외한다")
        void skipMissingReply() {
            // given
            given(notificationRepository.findByReceiverIdOrderByIdDesc(RECEIVER_ID, pageable))
                    .willReturn(new SliceImpl<>(List.of(savedReplyNotification(8L, savedUser(SENDER_ID, "노래하는 튜니"), 903L)), pageable, false));
            given(feedMessageReplyRepository.findAllByIdIn(List.of(903L))).willReturn(List.of());

            // when
            NotificationListResponse response = notificationService.getNotifications(RECEIVER_ID, null, SIZE);

            // then
            assertThat(response.getNotifications()).isEmpty();
        }
    }

    @Nested
    @DisplayName("알림 전체 삭제")
    class DeleteAllNotifications {

        @Test
        @DisplayName("내가 받은 알림을 모두 삭제하고 피드 메시지는 삭제하지 않는다")
        void success() {
            // when
            notificationService.deleteNotifications(RECEIVER_ID);

            // then
            verify(notificationRepository).deleteAllByReceiverId(RECEIVER_ID);
            verifyNoInteractions(feedMessageRepository);
        }
    }
}
