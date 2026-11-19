package com.tuneet.tuneetbe.domain.feed.service;

import com.tuneet.tuneetbe.domain.feed.dto.response.FeedMessageReplyDetailResponse;
import com.tuneet.tuneetbe.domain.feed.dto.response.FeedMessageReplySendResponse;
import com.tuneet.tuneetbe.domain.feed.entity.Feed;
import com.tuneet.tuneetbe.domain.feed.entity.FeedMessage;
import com.tuneet.tuneetbe.domain.feed.entity.FeedMessageReply;
import com.tuneet.tuneetbe.domain.feed.exception.FeedErrorCode;
import com.tuneet.tuneetbe.domain.feed.repository.FeedMessageReplyRepository;
import com.tuneet.tuneetbe.domain.feed.repository.FeedMessageRepository;
import com.tuneet.tuneetbe.domain.like.service.MusicLikeMarker;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.notification.entity.Notification;
import com.tuneet.tuneetbe.domain.notification.enums.NotificationType;
import com.tuneet.tuneetbe.domain.notification.event.PushEvent;
import com.tuneet.tuneetbe.domain.notification.repository.NotificationRepository;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserBlockRepository;
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
import org.springframework.context.ApplicationEventPublisher;
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
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FeedMessageReplyServiceTest {

    private static final Long OWNER_ID = 1L; // 피드 작성자
    private static final Long COMMENTER_ID = 2L; // 코멘트 작성자
    private static final Long MESSAGE_ID = 500L;
    private static final Long REPLY_ID = 900L;
    private static final String CONTENT = "추천 고마워요!";

    @Mock
    private FeedMessageReplyRepository feedMessageReplyRepository;

    @Mock
    private FeedMessageRepository feedMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MusicLikeMarker musicLikeMarker;

    @InjectMocks
    private FeedMessageReplyService feedMessageReplyService;

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
        ReflectionTestUtils.setField(music, "title", "not the same");
        ReflectionTestUtils.setField(music, "artist", "ROSÉ");
        ReflectionTestUtils.setField(music, "musicPicture", "cover.png");
        return music;
    }

    private FeedMessage savedMessage(User owner, User commenter) {
        Feed feed = Feed.builder().user(owner).music(savedMusic()).content("피드").build();
        ReflectionTestUtils.setField(feed, "id", 30L);
        FeedMessage message = FeedMessage.builder().user(commenter).feed(feed).music(savedMusic()).messageContent("이 노래도 좋아하실 것 같아요!").build();
        ReflectionTestUtils.setField(message, "id", MESSAGE_ID);
        return message;
    }

    private FeedMessageReply savedReply(Long id, FeedMessage message, User sender, User receiver) {
        FeedMessageReply reply = FeedMessageReply.builder()
                .feedMessage(message).sender(sender).receiver(receiver).music(savedMusic()).replyContent(CONTENT).build();
        ReflectionTestUtils.setField(reply, "id", id);
        return reply;
    }

    private void givenSaveReturnsReplyWithId(Long id) {
        given(feedMessageReplyRepository.save(any(FeedMessageReply.class))).willAnswer(invocation -> {
            FeedMessageReply reply = invocation.getArgument(0);
            ReflectionTestUtils.setField(reply, "id", id);
            return reply;
        });
    }

    @Nested
    @DisplayName("받은 코멘트에 답장 보내기")
    class ReplyToMessage {

        @Test
        @DisplayName("코멘트를 보낸 사람에게 답장을 저장하고 답장 알림과 푸시를 보낸다")
        void success() {
            // given
            User owner = savedUser(OWNER_ID, "튜니");
            User commenter = savedUser(COMMENTER_ID, "노래하는 튜니");
            given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(owner));
            given(feedMessageRepository.findById(MESSAGE_ID)).willReturn(Optional.of(savedMessage(owner, commenter)));
            givenSaveReturnsReplyWithId(REPLY_ID);

            // when
            FeedMessageReplySendResponse response = feedMessageReplyService.replyToMessage(OWNER_ID, MESSAGE_ID, savedMusic(), CONTENT);

            // then
            ArgumentCaptor<FeedMessageReply> replyCaptor = ArgumentCaptor.forClass(FeedMessageReply.class);
            verify(feedMessageReplyRepository).save(replyCaptor.capture());
            FeedMessageReply reply = replyCaptor.getValue();
            assertThat(reply.getSender()).isEqualTo(owner);
            assertThat(reply.getReceiver()).isEqualTo(commenter);
            assertThat(reply.getFeedMessage().getId()).isEqualTo(MESSAGE_ID);
            assertThat(reply.getReplyContent()).isEqualTo(CONTENT);

            ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue().getReceiver()).isEqualTo(commenter);
            assertThat(notificationCaptor.getValue().getType()).isEqualTo(NotificationType.FEED_MESSAGE_REPLY);
            assertThat(notificationCaptor.getValue().getTargetId()).isEqualTo(REPLY_ID);

            ArgumentCaptor<PushEvent> eventCaptor = ArgumentCaptor.forClass(PushEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().userId()).isEqualTo(COMMENTER_ID);
            assertThat(eventCaptor.getValue().type()).isEqualTo(NotificationType.FEED_MESSAGE_REPLY);
            assertThat(eventCaptor.getValue().targetId()).isEqualTo(REPLY_ID);

            assertThat(response.getReplyId()).isEqualTo(REPLY_ID);
            assertThat(response.getFeedMessageId()).isEqualTo(MESSAGE_ID);
            assertThat(response.getReceiverNickname()).isEqualTo("노래하는 튜니");
            assertThat(response.getMusic().getTitle()).isEqualTo("not the same");
            verify(musicLikeMarker).mark(OWNER_ID, List.of(response.getMusic()));
        }

        @Test
        @DisplayName("코멘트가 없으면 FEED_MESSAGE_NOT_FOUND 예외가 발생한다")
        void messageNotFound() {
            // given
            given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(savedUser(OWNER_ID, "튜니")));
            given(feedMessageRepository.findById(MESSAGE_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedMessageReplyService.replyToMessage(OWNER_ID, MESSAGE_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_MESSAGE_NOT_FOUND);
        }

        @Test
        @DisplayName("내 피드에 달린 코멘트가 아니면 FEED_MESSAGE_NOT_FOUND 예외가 발생하고 저장하지 않는다")
        void notMyFeed() {
            // given
            given(userRepository.findById(3L)).willReturn(Optional.of(savedUser(3L, "다른 사람")));
            given(feedMessageRepository.findById(MESSAGE_ID))
                    .willReturn(Optional.of(savedMessage(savedUser(OWNER_ID, "튜니"), savedUser(COMMENTER_ID, "노래하는 튜니"))));

            // when & then
            assertThatThrownBy(() -> feedMessageReplyService.replyToMessage(3L, MESSAGE_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_MESSAGE_NOT_FOUND);

            verify(feedMessageReplyRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 답장한 코멘트면 FEED_MESSAGE_ALREADY_REPLIED 예외가 발생하고 저장하지 않는다")
        void alreadyReplied() {
            // given
            User owner = savedUser(OWNER_ID, "튜니");
            given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(owner));
            given(feedMessageRepository.findById(MESSAGE_ID)).willReturn(Optional.of(savedMessage(owner, savedUser(COMMENTER_ID, "노래하는 튜니"))));
            given(feedMessageReplyRepository.existsByFeedMessageId(MESSAGE_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> feedMessageReplyService.replyToMessage(OWNER_ID, MESSAGE_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_MESSAGE_ALREADY_REPLIED);

            verify(feedMessageReplyRepository, never()).save(any());
        }

        @Test
        @DisplayName("코멘트를 보낸 사람이 탈퇴했으면 FEED_MESSAGE_REPLY_TO_DELETED_USER 예외가 발생한다")
        void deletedCommenter() {
            // given
            User owner = savedUser(OWNER_ID, "튜니");
            User commenter = savedUser(COMMENTER_ID, "노래하는 튜니");
            commenter.deleteUser(LocalDateTime.now());
            given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(owner));
            given(feedMessageRepository.findById(MESSAGE_ID)).willReturn(Optional.of(savedMessage(owner, commenter)));

            // when & then
            assertThatThrownBy(() -> feedMessageReplyService.replyToMessage(OWNER_ID, MESSAGE_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_MESSAGE_REPLY_TO_DELETED_USER);

            verify(feedMessageReplyRepository, never()).save(any());
        }

        @Test
        @DisplayName("코멘트를 보낸 사람과 차단 관계면 BLOCKED_USER 예외가 발생하고 알림도 보내지 않는다")
        void blocked() {
            // given
            User owner = savedUser(OWNER_ID, "튜니");
            given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(owner));
            given(feedMessageRepository.findById(MESSAGE_ID)).willReturn(Optional.of(savedMessage(owner, savedUser(COMMENTER_ID, "노래하는 튜니"))));
            given(userBlockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(OWNER_ID, COMMENTER_ID, COMMENTER_ID, OWNER_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> feedMessageReplyService.replyToMessage(OWNER_ID, MESSAGE_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.BLOCKED_USER);

            verify(feedMessageReplyRepository, never()).save(any());
            verifyNoInteractions(notificationRepository, eventPublisher);
        }
    }

    @Nested
    @DisplayName("받은 답장에 다시 답장 보내기")
    class ReplyToReply {

        @Test
        @DisplayName("답장을 보낸 사람에게 같은 코멘트로 이어서 답장을 저장한다")
        void success() {
            // given
            User owner = savedUser(OWNER_ID, "튜니");
            User commenter = savedUser(COMMENTER_ID, "노래하는 튜니");
            FeedMessage message = savedMessage(owner, commenter);
            given(userRepository.findById(COMMENTER_ID)).willReturn(Optional.of(commenter));
            given(feedMessageReplyRepository.findById(REPLY_ID)).willReturn(Optional.of(savedReply(REPLY_ID, message, owner, commenter)));
            givenSaveReturnsReplyWithId(901L);

            // when
            FeedMessageReplySendResponse response = feedMessageReplyService.replyToReply(COMMENTER_ID, REPLY_ID, savedMusic(), CONTENT);

            // then
            ArgumentCaptor<FeedMessageReply> captor = ArgumentCaptor.forClass(FeedMessageReply.class);
            verify(feedMessageReplyRepository).save(captor.capture());
            assertThat(captor.getValue().getSender()).isEqualTo(commenter);
            assertThat(captor.getValue().getReceiver()).isEqualTo(owner);
            assertThat(captor.getValue().getFeedMessage()).isEqualTo(message);
            assertThat(response.getReplyId()).isEqualTo(901L);
            assertThat(response.getReceiverNickname()).isEqualTo("튜니");

            ArgumentCaptor<PushEvent> eventCaptor = ArgumentCaptor.forClass(PushEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().userId()).isEqualTo(OWNER_ID);
            assertThat(eventCaptor.getValue().targetId()).isEqualTo(901L);
            verify(musicLikeMarker).mark(COMMENTER_ID, List.of(response.getMusic()));
        }

        @Test
        @DisplayName("내가 받은 답장이 아니면 FEED_MESSAGE_REPLY_NOT_FOUND 예외가 발생한다")
        void notMyReply() {
            // given
            User owner = savedUser(OWNER_ID, "튜니");
            User commenter = savedUser(COMMENTER_ID, "노래하는 튜니");
            given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(owner));
            given(feedMessageReplyRepository.findById(REPLY_ID))
                    .willReturn(Optional.of(savedReply(REPLY_ID, savedMessage(owner, commenter), owner, commenter)));

            // when & then
            assertThatThrownBy(() -> feedMessageReplyService.replyToReply(OWNER_ID, REPLY_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_MESSAGE_REPLY_NOT_FOUND);

            verify(feedMessageReplyRepository, never()).save(any());
        }

        @Test
        @DisplayName("같은 코멘트에 더 나중 답장이 있으면 FEED_MESSAGE_ALREADY_REPLIED 예외가 발생한다")
        void alreadyReplied() {
            // given
            User owner = savedUser(OWNER_ID, "튜니");
            User commenter = savedUser(COMMENTER_ID, "노래하는 튜니");
            given(userRepository.findById(COMMENTER_ID)).willReturn(Optional.of(commenter));
            given(feedMessageReplyRepository.findById(REPLY_ID))
                    .willReturn(Optional.of(savedReply(REPLY_ID, savedMessage(owner, commenter), owner, commenter)));
            given(feedMessageReplyRepository.existsByFeedMessageIdAndIdGreaterThan(MESSAGE_ID, REPLY_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> feedMessageReplyService.replyToReply(COMMENTER_ID, REPLY_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_MESSAGE_ALREADY_REPLIED);

            verify(feedMessageReplyRepository, never()).save(any());
        }

        @Test
        @DisplayName("답장을 보낸 사람이 탈퇴했으면 FEED_MESSAGE_REPLY_TO_DELETED_USER 예외가 발생한다")
        void deletedSender() {
            // given
            User owner = savedUser(OWNER_ID, "튜니");
            owner.deleteUser(LocalDateTime.now());
            User commenter = savedUser(COMMENTER_ID, "노래하는 튜니");
            given(userRepository.findById(COMMENTER_ID)).willReturn(Optional.of(commenter));
            given(feedMessageReplyRepository.findById(REPLY_ID))
                    .willReturn(Optional.of(savedReply(REPLY_ID, savedMessage(owner, commenter), owner, commenter)));

            // when & then
            assertThatThrownBy(() -> feedMessageReplyService.replyToReply(COMMENTER_ID, REPLY_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_MESSAGE_REPLY_TO_DELETED_USER);
        }
    }

    @Nested
    @DisplayName("받은 답장 상세보기")
    class GetReply {

        @Test
        @DisplayName("원 피드, 원본 코멘트, 주고받은 답장 전체를 오래된 순으로 반환한다")
        void success() {
            // given
            User owner = savedUser(OWNER_ID, "튜니");
            User commenter = savedUser(COMMENTER_ID, "노래하는 튜니");
            FeedMessage message = savedMessage(owner, commenter);
            FeedMessageReply first = savedReply(REPLY_ID, message, owner, commenter);
            FeedMessageReply second = savedReply(901L, message, commenter, owner);
            FeedMessageReply third = savedReply(902L, message, owner, commenter);
            given(feedMessageReplyRepository.findWithDetailById(902L)).willReturn(Optional.of(third));
            given(feedMessageReplyRepository.findAllByFeedMessageIdOrderByIdAsc(MESSAGE_ID)).willReturn(List.of(first, second, third));

            // when
            FeedMessageReplyDetailResponse response = feedMessageReplyService.getReply(COMMENTER_ID, 902L);

            // then
            assertThat(response.getReplyId()).isEqualTo(902L);
            assertThat(response.getFeedId()).isEqualTo(30L);
            assertThat(response.getIsReplied()).isFalse();
            assertThat(response.getOriginalMessage().getFeedMessageId()).isEqualTo(MESSAGE_ID);
            assertThat(response.getOriginalMessage().getSenderNickname()).isEqualTo("노래하는 튜니");
            assertThat(response.getOriginalMessage().getIsMine()).isTrue();
            assertThat(response.getOriginalMessage().getMessageContent()).isEqualTo("이 노래도 좋아하실 것 같아요!");
            assertThat(response.getOriginalMessage().getMusic().getTitle()).isEqualTo("not the same");
            assertThat(response.getReplies()).extracting(FeedMessageReplyDetailResponse.Reply::getReplyId).containsExactly(REPLY_ID, 901L, 902L);
            assertThat(response.getReplies()).extracting(FeedMessageReplyDetailResponse.Reply::getIsMine).containsExactly(false, true, false);
            assertThat(response.getReplies().get(0).getSenderNickname()).isEqualTo("튜니");
            assertThat(response.getReplies().get(0).getReplyContent()).isEqualTo(CONTENT);
        }

        @Test
        @DisplayName("조회한 답장보다 나중 답장이 있으면 isReplied가 true다")
        void replied() {
            // given
            User owner = savedUser(OWNER_ID, "튜니");
            User commenter = savedUser(COMMENTER_ID, "노래하는 튜니");
            FeedMessage message = savedMessage(owner, commenter);
            FeedMessageReply first = savedReply(REPLY_ID, message, owner, commenter);
            given(feedMessageReplyRepository.findWithDetailById(REPLY_ID)).willReturn(Optional.of(first));
            given(feedMessageReplyRepository.findAllByFeedMessageIdOrderByIdAsc(MESSAGE_ID))
                    .willReturn(List.of(first, savedReply(901L, message, commenter, owner)));

            // when
            FeedMessageReplyDetailResponse response = feedMessageReplyService.getReply(COMMENTER_ID, REPLY_ID);

            // then
            assertThat(response.getIsReplied()).isTrue();
        }

        @Test
        @DisplayName("탈퇴한 사용자가 보낸 코멘트와 답장은 탈퇴한 사용자로 표시한다")
        void deletedSender() {
            // given
            User owner = savedUser(OWNER_ID, "튜니");
            owner.deleteUser(LocalDateTime.now());
            User commenter = savedUser(COMMENTER_ID, "노래하는 튜니");
            FeedMessageReply reply = savedReply(REPLY_ID, savedMessage(owner, commenter), owner, commenter);
            given(feedMessageReplyRepository.findWithDetailById(REPLY_ID)).willReturn(Optional.of(reply));
            given(feedMessageReplyRepository.findAllByFeedMessageIdOrderByIdAsc(MESSAGE_ID)).willReturn(List.of(reply));

            // when
            FeedMessageReplyDetailResponse response = feedMessageReplyService.getReply(COMMENTER_ID, REPLY_ID);

            // then
            assertThat(response.getReplies().get(0).getSenderNickname()).isEqualTo("탈퇴한 사용자");
        }

        @Test
        @DisplayName("노래 없이 보낸 원본 코멘트는 노래가 null이다")
        void messageWithoutMusic() {
            // given
            User owner = savedUser(OWNER_ID, "튜니");
            User commenter = savedUser(COMMENTER_ID, "노래하는 튜니");
            FeedMessage message = savedMessage(owner, commenter);
            ReflectionTestUtils.setField(message, "music", null);
            FeedMessageReply reply = savedReply(REPLY_ID, message, owner, commenter);
            given(feedMessageReplyRepository.findWithDetailById(REPLY_ID)).willReturn(Optional.of(reply));
            given(feedMessageReplyRepository.findAllByFeedMessageIdOrderByIdAsc(MESSAGE_ID)).willReturn(List.of(reply));

            // when
            FeedMessageReplyDetailResponse response = feedMessageReplyService.getReply(COMMENTER_ID, REPLY_ID);

            // then
            assertThat(response.getOriginalMessage().getMusic()).isNull();
        }

        @Test
        @DisplayName("내가 받은 답장이 아니면 FEED_MESSAGE_REPLY_NOT_FOUND 예외가 발생한다")
        void notMine() {
            // given
            User owner = savedUser(OWNER_ID, "튜니");
            User commenter = savedUser(COMMENTER_ID, "노래하는 튜니");
            given(feedMessageReplyRepository.findWithDetailById(REPLY_ID))
                    .willReturn(Optional.of(savedReply(REPLY_ID, savedMessage(owner, commenter), owner, commenter)));

            // when & then
            assertThatThrownBy(() -> feedMessageReplyService.getReply(OWNER_ID, REPLY_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(FeedErrorCode.FEED_MESSAGE_REPLY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("코멘트에 달린 답장 삭제")
    class DeleteRepliesOf {

        @Test
        @DisplayName("답장 알림을 먼저 삭제한 뒤 답장을 삭제한다")
        void success() {
            // given
            given(feedMessageReplyRepository.findIdsByFeedMessageIdIn(List.of(MESSAGE_ID))).willReturn(List.of(900L, 901L));

            // when
            feedMessageReplyService.deleteRepliesOf(List.of(MESSAGE_ID));

            // then
            InOrder inOrder = inOrder(notificationRepository, feedMessageReplyRepository);
            inOrder.verify(notificationRepository).deleteAllByTypeAndTargetIdIn(NotificationType.FEED_MESSAGE_REPLY, List.of(900L, 901L));
            inOrder.verify(feedMessageReplyRepository).deleteAllByFeedMessageIdIn(List.of(MESSAGE_ID));
        }

        @Test
        @DisplayName("코멘트가 없으면 아무것도 하지 않는다")
        void empty() {
            // when
            feedMessageReplyService.deleteRepliesOf(List.of());

            // then
            verifyNoInteractions(feedMessageReplyRepository, notificationRepository);
        }
    }
}
