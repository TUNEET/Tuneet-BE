package com.tuneet.tuneetbe.domain.notification.service;

import com.tuneet.tuneetbe.domain.feed.entity.FeedMessage;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    private static final String DELETED_USER_NICKNAME = "탈퇴한 사용자"; // 탈퇴한 사용자 표시 닉네임

    private final NotificationRepository notificationRepository;
    private final FeedMessageRepository feedMessageRepository;

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

        // 피드 메시지 알림의 대상 메시지 ID 모으기 (알림의 targetId 한 번에 조회)
        List<Long> messageIds = new ArrayList<>();
        for (Notification notification : notifications.getContent()) {
            if (notification.getType() == NotificationType.FEED_MESSAGE) {
                messageIds.add(notification.getTargetId());
            }
        }

        // 메시지를 한 번에 조회
        List<FeedMessage> feedMessages = new ArrayList<>();
        if (!messageIds.isEmpty()) {
            feedMessages = feedMessageRepository.findAllByIdIn(messageIds);
        }

        // 응답 목록 세팅
        List<NotificationResponse> notificationResponses = new ArrayList<>();
        for (Notification notification : notifications.getContent()) {

            // 피드 메시지 알림이 아니면 건너뜀
            if (notification.getType() != NotificationType.FEED_MESSAGE) {
                continue;
            }

            // 알림의 대상 메시지 찾기
            FeedMessage feedMessage = null;
            for (FeedMessage message : feedMessages) {
                if (message.getId().equals(notification.getTargetId())) {
                    feedMessage = message;
                    break;
                }
            }

            // 대상 메시지가 없는 알림은 건너뜀
            if (feedMessage == null) {
                continue;
            }

            notificationResponses.add(toNotificationResponse(notification, feedMessage));
        }

        // 응답 반환
        return NotificationListResponse.builder()
                .notifications(notificationResponses)
                .hasNext(notifications.hasNext())
                .build();
    }

    // 알림과 메시지를 응답 dto로 변환
    private NotificationResponse toNotificationResponse(Notification notification, FeedMessage feedMessage) {

        User sender = notification.getSender(); // 보낸 사람
        Music music = feedMessage.getMusic(); // 메시지와 함께 보낸 노래

        // 탈퇴한 사용자가 보낸 알림은 "탈퇴한 사용자"로 표시
        String senderNickname = sender.isDeleted() ? DELETED_USER_NICKNAME : sender.getNickname();

        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .type(notification.getType())
                .senderNickname(senderNickname)
                .feedId(feedMessage.getFeed().getId())
                .messageContent(feedMessage.getMessageContent())
                .music(MusicResponse.builder()
                        .musicId(music.getId())
                        .title(music.getTitle())
                        .artist(music.getArtist())
                        .musicPicture(music.getMusicPicture())
                        .build())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
