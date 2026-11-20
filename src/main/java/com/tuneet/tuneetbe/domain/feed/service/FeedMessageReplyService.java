package com.tuneet.tuneetbe.domain.feed.service;

import com.tuneet.tuneetbe.domain.feed.dto.response.FeedMessageReplyDetailResponse;
import com.tuneet.tuneetbe.domain.feed.dto.response.FeedMessageReplySendResponse;
import com.tuneet.tuneetbe.domain.feed.entity.FeedMessage;
import com.tuneet.tuneetbe.domain.feed.entity.FeedMessageReply;
import com.tuneet.tuneetbe.domain.feed.exception.FeedErrorCode;
import com.tuneet.tuneetbe.domain.feed.repository.FeedMessageReplyRepository;
import com.tuneet.tuneetbe.domain.feed.repository.FeedMessageRepository;
import com.tuneet.tuneetbe.domain.like.service.MusicLikeMarker;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeedMessageReplyService {

    private static final String DELETED_USER_NICKNAME = "탈퇴한 사용자"; // 탈퇴한 사용자 표시 닉네임

    private final FeedMessageReplyRepository feedMessageReplyRepository;
    private final FeedMessageRepository feedMessageRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MusicLikeMarker musicLikeMarker;

    // 받은 코멘트에 답장 보내기 (피드 작성자 -> 코멘트 작성자)
    @Transactional
    public FeedMessageReplySendResponse replyToMessage(Long userId, Long feedMessageId, Music music, String replyContent) {

        // 보내는 사람 조회
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 답장 대상 코멘트 조회
        FeedMessage feedMessage = feedMessageRepository.findById(feedMessageId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_MESSAGE_NOT_FOUND));

        // 내 피드에 달린 코멘트인지 확인
        if (!feedMessage.getFeed().getUser().getId().equals(userId)) {
            log.warn("[FeedMessageReplyService] 답장할 수 없는 코멘트입니다.");
            throw new CustomException(FeedErrorCode.FEED_MESSAGE_NOT_FOUND);
        }

        // 이미 답장한 코멘트인지 확인 (코멘트에는 피드 작성자만 한 번 답장할 수 있음)
        if (feedMessageReplyRepository.existsByFeedMessageId(feedMessageId)) {
            log.warn("[FeedMessageReplyService] 이미 답장한 코멘트입니다.");
            throw new CustomException(FeedErrorCode.FEED_MESSAGE_ALREADY_REPLIED);
        }

        // 받는 사람은 코멘트를 보낸 사람
        return sendReply(feedMessage, sender, feedMessage.getUser(), music, replyContent);
    }

    // 받은 답장에 다시 답장 보내기
    @Transactional
    public FeedMessageReplySendResponse replyToReply(Long userId, Long replyId, Music music, String replyContent) {

        // 보내는 사람 조회
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 답장 대상 답장 조회
        FeedMessageReply parentReply = feedMessageReplyRepository.findById(replyId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_MESSAGE_REPLY_NOT_FOUND));

        // 내가 받은 답장인지 확인
        if (!parentReply.getReceiver().getId().equals(userId)) {
            log.warn("[FeedMessageReplyService] 답장할 수 없는 답장입니다.");
            throw new CustomException(FeedErrorCode.FEED_MESSAGE_REPLY_NOT_FOUND);
        }

        // 이미 답장한 답장인지 확인 (같은 코멘트에 더 나중 답장이 있으면 이미 답장한 것)
        FeedMessage feedMessage = parentReply.getFeedMessage();
        if (feedMessageReplyRepository.existsByFeedMessageIdAndIdGreaterThan(feedMessage.getId(), replyId)) {
            log.warn("[FeedMessageReplyService] 이미 답장한 답장입니다.");
            throw new CustomException(FeedErrorCode.FEED_MESSAGE_ALREADY_REPLIED);
        }

        // 받는 사람은 답장을 보낸 사람
        return sendReply(feedMessage, sender, parentReply.getSender(), music, replyContent);
    }

    // 받은 답장 상세보기
    public FeedMessageReplyDetailResponse getReply(Long userId, Long replyId) {

        // 답장 조회
        FeedMessageReply reply = feedMessageReplyRepository.findWithDetailById(replyId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_MESSAGE_REPLY_NOT_FOUND));

        // 내가 받은 답장인지 확인
        if (!reply.getReceiver().getId().equals(userId)) {
            log.warn("[FeedMessageReplyService] 조회할 수 없는 답장입니다.");
            throw new CustomException(FeedErrorCode.FEED_MESSAGE_REPLY_NOT_FOUND);
        }

        FeedMessage feedMessage = reply.getFeedMessage(); // 원본 코멘트

        // 원본 코멘트에 주고받은 답장 전체 (오래된 순)
        List<FeedMessageReply> threadReplies = feedMessageReplyRepository.findAllByFeedMessageIdOrderByIdAsc(feedMessage.getId());

        // 내가 이 답장에 이미 답장했는지 (더 나중 답장이 있으면 답장한 것, 답장 버튼 표시용)
        Long latestReplyId = threadReplies.get(threadReplies.size() - 1).getId();
        boolean isReplied = latestReplyId > replyId;

        // 원본 코멘트, 주고받은 답장 세팅
        ReplyThread thread = toThread(userId, feedMessage, threadReplies);

        // 응답 반환
        return FeedMessageReplyDetailResponse.builder()
                .replyId(replyId)
                .feedId(feedMessage.getFeed().getId())
                .isReplied(isReplied)
                .originalMessage(thread.originalMessage())
                .replies(thread.replies())
                .build();
    }

    // 원본 코멘트와 주고받은 답장
    private record ReplyThread(FeedMessageReplyDetailResponse.OriginalMessage originalMessage, List<FeedMessageReplyDetailResponse.Reply> replies) {
    }

    // 원본 코멘트와 답장 목록을 응답 dto로 변환하고 내가 좋아요한 곡 표시
    private ReplyThread toThread(Long userId, FeedMessage feedMessage, List<FeedMessageReply> threadReplies) {

        // 원본 코멘트 세팅
        User commenter = feedMessage.getUser();
        MusicResponse messageMusic = null;
        if (feedMessage.getMusic() != null) {
            messageMusic = toMusicResponse(feedMessage.getMusic());
        }
        FeedMessageReplyDetailResponse.OriginalMessage originalMessage = FeedMessageReplyDetailResponse.OriginalMessage.builder()
                .feedMessageId(feedMessage.getId())
                .senderId(commenter.getId())
                .senderNickname(toNickname(commenter))
                .senderProfilePicture(commenter.getProfilePicture())
                .isMine(commenter.getId().equals(userId))
                .messageContent(feedMessage.getMessageContent())
                .music(messageMusic)
                .createdAt(feedMessage.getCreatedAt())
                .build();

        // 주고받은 답장 세팅
        List<FeedMessageReplyDetailResponse.Reply> replies = new ArrayList<>();
        for (FeedMessageReply threadReply : threadReplies) {
            User sender = threadReply.getSender();
            replies.add(FeedMessageReplyDetailResponse.Reply.builder()
                    .replyId(threadReply.getId())
                    .senderId(sender.getId())
                    .senderNickname(toNickname(sender))
                    .senderProfilePicture(sender.getProfilePicture())
                    .isMine(sender.getId().equals(userId))
                    .music(toMusicResponse(threadReply.getMusic()))
                    .replyContent(threadReply.getReplyContent())
                    .createdAt(threadReply.getCreatedAt())
                    .build());
        }

        // 내가 좋아요한 곡 표시 (코멘트, 답장의 곡 전체를 한 번에 조회)
        List<MusicResponse> musics = new ArrayList<>();
        if (messageMusic != null) {
            musics.add(messageMusic);
        }
        for (FeedMessageReplyDetailResponse.Reply r : replies) {
            musics.add(r.getMusic());
        }
        musicLikeMarker.mark(userId, musics);

        return new ReplyThread(originalMessage, replies);
    }

    // 탈퇴한 사용자는 "탈퇴한 사용자"로 표시
    private String toNickname(User user) {
        return user.isDeleted() ? DELETED_USER_NICKNAME : user.getNickname();
    }

    // 코멘트에 달린 답장과 답장 알림 삭제 (코멘트를 지우기 전에 호출)
    @Transactional
    public void deleteRepliesOf(List<Long> feedMessageIds) {

        if (feedMessageIds.isEmpty()) {
            return;
        }

        // 답장 알림 삭제 (답장이 지워지면 알림창에 보여줄 내용이 없어지기 때문)
        List<Long> replyIds = feedMessageReplyRepository.findIdsByFeedMessageIdIn(feedMessageIds);
        if (!replyIds.isEmpty()) {
            notificationRepository.deleteAllByTypeAndTargetIdIn(NotificationType.FEED_MESSAGE_REPLY, replyIds);
        }

        // 답장 삭제
        feedMessageReplyRepository.deleteAllByFeedMessageIdIn(feedMessageIds);
    }

    // 답장 저장, 알림 저장, 푸시 전송
    private FeedMessageReplySendResponse sendReply(FeedMessage feedMessage, User sender, User receiver, Music music, String replyContent) {

        // 사용자가 탈퇴했는지 확인
        if (receiver.isDeleted()) {
            log.warn("[FeedMessageReplyService] 탈퇴한 사용자에게는 답장을 보낼 수 없습니다.");
            throw new CustomException(FeedErrorCode.FEED_MESSAGE_REPLY_TO_DELETED_USER);
        }

        // 차단 관계인 사용자인지 확인 (차단 전에 받은 메시지라도 답장은 보낼 수 없음)
        if (userBlockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(sender.getId(), receiver.getId(), receiver.getId(), sender.getId())) {
            log.warn("[FeedMessageReplyService] 차단 관계인 사용자에게는 답장을 보낼 수 없습니다.");
            throw new CustomException(UserErrorCode.BLOCKED_USER);
        }

        // DB 저장
        FeedMessageReply reply = feedMessageReplyRepository.save(FeedMessageReply.builder()
                .feedMessage(feedMessage)
                .sender(sender)
                .receiver(receiver)
                .music(music)
                .replyContent(replyContent)
                .build());

        // 알림 저장
        notificationRepository.save(Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .type(NotificationType.FEED_MESSAGE_REPLY)
                .targetId(reply.getId())
                .build());

        // 받는 사람에게 푸시 알림 (커밋 후 전송, 알림을 누르면 답장 상세로 이동)
        eventPublisher.publishEvent(new PushEvent(receiver.getId(), "답장이 도착했어요", sender.getNickname() + "님이 답장을 보냈어요", NotificationType.FEED_MESSAGE_REPLY, reply.getId(), sender.getNickname(), reply.getCreatedAt()));

        // 로그 출력
        log.info("[FeedMessageReplyService] 코멘트 답장 성공: replyId = {}, feedMessageId = {}", reply.getId(), feedMessage.getId());

        FeedMessageReplySendResponse response = FeedMessageReplySendResponse.builder()
                .replyId(reply.getId())
                .feedMessageId(feedMessage.getId())
                .receiverNickname(receiver.getNickname())
                .music(toMusicResponse(music))
                .replyContent(reply.getReplyContent())
                .createdAt(reply.getCreatedAt())
                .build();

        // 내가 좋아요한 곡 표시
        musicLikeMarker.mark(sender.getId(), List.of(response.getMusic()));

        // 응답 반환
        return response;
    }

    private MusicResponse toMusicResponse(Music music) {
        return MusicResponse.builder()
                .musicId(music.getExternalId())
                .title(music.getTitle())
                .artist(music.getArtist())
                .musicPicture(music.getMusicPicture())
                .videoId(music.getYoutubeVideoId())
                .build();
    }
}
