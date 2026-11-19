package com.tuneet.tuneetbe.domain.notification.service;

import com.tuneet.tuneetbe.domain.like.service.MusicLikeMarker;
import com.tuneet.tuneetbe.domain.feed.entity.FeedMessage;
import com.tuneet.tuneetbe.domain.feed.entity.FeedMessageReply;
import com.tuneet.tuneetbe.domain.feed.repository.FeedMessageReplyRepository;
import com.tuneet.tuneetbe.domain.feed.repository.FeedMessageRepository;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.notification.dto.response.NotificationListResponse;
import com.tuneet.tuneetbe.domain.notification.dto.response.NotificationResponse;
import com.tuneet.tuneetbe.domain.notification.entity.Notification;
import com.tuneet.tuneetbe.domain.notification.enums.NotificationType;
import com.tuneet.tuneetbe.domain.notification.repository.NotificationRepository;
import com.tuneet.tuneetbe.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    private static final String DELETED_USER_NICKNAME = "탈퇴한 사용자"; // 탈퇴한 사용자 표시 닉네임

    private final NotificationRepository notificationRepository;
    private final FeedMessageRepository feedMessageRepository;
    private final FeedMessageReplyRepository feedMessageReplyRepository;
    private final MusicLikeMarker musicLikeMarker;

    // 알림 목록 조회
    public NotificationListResponse getNotifications(Long userId, Long cursor, int size) {

        Pageable pageable = PageRequest.of(0, size);
        Slice<Notification> notifications;

        // 커서가 없으면 내가 받은 최신 알림부터 조회
        if (cursor == null) {
            notifications = notificationRepository.findByReceiverIdOrderByIdDesc(userId, pageable);
        }

        // 커서가 있으면 커서보다 이전 알림 조회
        else {
            notifications = notificationRepository.findByReceiverIdAndIdLessThanOrderByIdDesc(userId, cursor, pageable);
        }

        // 피드 메시지 알림과 답장 알림의 대상 ID 모으기 (알림의 targetId 한 번에 조회)
        List<Long> messageIds = new ArrayList<>();
        List<Long> replyIds = new ArrayList<>();
        for (Notification notification : notifications.getContent()) {
            if (notification.getType() == NotificationType.FEED_MESSAGE) {
                messageIds.add(notification.getTargetId());
            }
            else if (notification.getType() == NotificationType.FEED_MESSAGE_REPLY) {
                replyIds.add(notification.getTargetId());
            }
        }

        // 메시지와 답장을 한 번에 조회
        Map<Long, FeedMessage> feedMessages = new HashMap<>();
        if (!messageIds.isEmpty()) {
            for (FeedMessage message : feedMessageRepository.findAllByIdIn(messageIds)) {
                feedMessages.put(message.getId(), message);
            }
        }
        Map<Long, FeedMessageReply> replies = new HashMap<>();
        if (!replyIds.isEmpty()) {
            for (FeedMessageReply reply : feedMessageReplyRepository.findAllByIdIn(replyIds)) {
                replies.put(reply.getId(), reply);
            }
        }

        // 코멘트별 가장 최근 답장 ID 조회 (이미 답장했는지 확인용)
        Set<Long> threadIds = new HashSet<>(feedMessages.keySet());
        for (FeedMessageReply reply : replies.values()) {
            threadIds.add(reply.getFeedMessage().getId());
        }
        Map<Long, Long> latestReplyIds = new HashMap<>();
        if (!threadIds.isEmpty()) {
            for (Object[] row : feedMessageReplyRepository.findLatestReplyIds(new ArrayList<>(threadIds))) {
                latestReplyIds.put((Long) row[0], (Long) row[1]);
            }
        }

        // 응답 목록 세팅
        List<NotificationResponse> notificationResponses = new ArrayList<>();
        for (Notification notification : notifications.getContent()) {

            // 피드 메시지 알림: 답장이 하나라도 있으면 이미 답장한 것
            if (notification.getType() == NotificationType.FEED_MESSAGE) {
                FeedMessage feedMessage = feedMessages.get(notification.getTargetId());

                // 대상 메시지가 없는 알림은 건너뜀
                if (feedMessage == null) {
                    continue;
                }

                boolean isReplied = latestReplyIds.containsKey(feedMessage.getId());
                notificationResponses.add(toNotificationResponse(notification, feedMessage, null, feedMessage.getMessageContent(), feedMessage.getMusic(), isReplied));
            }

            // 답장 알림: 같은 코멘트에 더 나중 답장이 있으면 이미 답장한 것
            else if (notification.getType() == NotificationType.FEED_MESSAGE_REPLY) {
                FeedMessageReply reply = replies.get(notification.getTargetId());

                // 대상 답장이 없는 알림은 건너뜀
                if (reply == null) {
                    continue;
                }

                FeedMessage feedMessage = reply.getFeedMessage();
                boolean isReplied = latestReplyIds.getOrDefault(feedMessage.getId(), reply.getId()) > reply.getId();
                notificationResponses.add(toNotificationResponse(notification, feedMessage, reply.getId(), reply.getReplyContent(), reply.getMusic(), isReplied));
            }
        }

        // 내가 좋아요한 곡 표시
        musicLikeMarker.mark(userId, notificationResponses.stream().map(NotificationResponse::getMusic).toList());

        // 응답 반환
        return NotificationListResponse.builder()
                .notifications(notificationResponses)
                .hasNext(notifications.hasNext())
                .build();
    }

    // 알림과 메시지를 응답 dto로 변환
    private NotificationResponse toNotificationResponse(Notification notification, FeedMessage feedMessage, Long replyId, String content, Music music, boolean isReplied) {

        User sender = notification.getSender(); // 보낸 사람

        // 탈퇴한 사용자가 보낸 알림은 "탈퇴한 사용자"로 표시
        String senderNickname = sender.isDeleted() ? DELETED_USER_NICKNAME : sender.getNickname();

        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .type(notification.getType())
                .senderNickname(senderNickname)
                .feedId(feedMessage.getFeed().getId())
                .feedMessageId(feedMessage.getId())
                .replyId(replyId)
                .messageContent(content)
                .music(toMusicResponse(music))
                .isRead(notification.getIsRead())
                .isReplied(isReplied)
                .createdAt(notification.getCreatedAt())
                .build();
    }

    // 노래를 응답 dto로 변환 (노래 없이 보낸 메시지면 null)
    private MusicResponse toMusicResponse(Music music) {

        if (music == null) {
            return null;
        }

        return MusicResponse.builder()
                .musicId(music.getExternalId())
                .title(music.getTitle())
                .artist(music.getArtist())
                .musicPicture(music.getMusicPicture())
                .build();
    }

    // 알림 전체 삭제
    @Transactional
    public void deleteNotifications(Long userId) {

        // 알림 삭제
        notificationRepository.deleteAllByReceiverId(userId);

        // 로그 출력
        log.info("[NotificationService] 알림 전체 삭제 성공: userId = {}", userId);
    }
}
