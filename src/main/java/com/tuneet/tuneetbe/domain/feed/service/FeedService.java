package com.tuneet.tuneetbe.domain.feed.service;

import com.tuneet.tuneetbe.domain.feed.dto.response.*;
import com.tuneet.tuneetbe.domain.feed.entity.Feed;
import com.tuneet.tuneetbe.domain.feed.entity.FeedMessage;
import com.tuneet.tuneetbe.domain.feed.exception.FeedErrorCode;
import com.tuneet.tuneetbe.domain.feed.repository.FeedMessageRepository;
import com.tuneet.tuneetbe.domain.feed.repository.FeedRepository;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.notification.entity.Notification;
import com.tuneet.tuneetbe.domain.notification.enums.NotificationType;
import com.tuneet.tuneetbe.domain.notification.event.PushEvent;
import com.tuneet.tuneetbe.domain.notification.repository.NotificationRepository;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
public class FeedService {

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final FeedMessageRepository feedMessageRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 피드 작성
    @Transactional
    public FeedCreateResponse createFeed(Long userId, Music music, String content) {

        // 로그인한 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // DB 저장
        Feed feed = feedRepository.save(Feed.builder()
                .user(user)
                .music(music)
                .content(content)
                .build());

        // 로그 출력
        log.info("[FeedService] 피드 작성 성공: feedId = {}", feed.getId());

        // 응답 반환
        return FeedCreateResponse.builder()
                .feedId(feed.getId())
                .nickname(user.getNickname())
                .music(MusicResponse.builder()
                        .musicId(music.getId())
                        .title(music.getTitle())
                        .artist(music.getArtist())
                        .musicPicture(music.getMusicPicture())
                        .build())
                .content(feed.getContent())
                .createdAt(feed.getCreatedAt())
                .build();
    }

    // 피드 상세보기
    public FeedDetailResponse getFeed(Long userId, Long feedId) {

        // 피드가 존재하는지 조회
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        // 응답 반환
        return toFeedDetailResponse(userId, feed);
    }

    // Feed 엔티티를 응답 dto로 변환
    private FeedDetailResponse toFeedDetailResponse(Long userId, Feed feed) {

        User writer = feed.getUser(); // 작성자
        Music music = feed.getMusic(); // 피드에 있는 음악

        return FeedDetailResponse.builder()
                .feedId(feed.getId())
                .nickname(writer.getNickname())
                .profilePicture(writer.getProfilePicture())
                .music(MusicResponse.builder()
                        .musicId(music.getId())
                        .title(music.getTitle())
                        .artist(music.getArtist())
                        .musicPicture(music.getMusicPicture())
                        .build())
                .content(feed.getContent())
                .createdAt(feed.getCreatedAt())
                .isMine(writer.getId().equals(userId))
                .build();
    }

    // 피드 리스트 보기
    public FeedListResponse getFeeds(Long userId, Long cursor, int size) {

        Pageable pageable = PageRequest.of(0, size);
        Slice<Feed> feeds;

        // 커서가 없으면 최신 피드부터 조회
        if (cursor == null) {
            feeds = feedRepository.findAllByOrderByIdDesc(pageable);
        }
        else {
            // 커서가 있으면 커서보다 이전 피드 조회
            feeds = feedRepository.findByIdLessThanOrderByIdDesc(cursor, pageable);
        }

        // 응답 목록 세팅
        List<FeedDetailResponse> feedResponses = new ArrayList<>();
        for (Feed feed : feeds.getContent()) {
            feedResponses.add(toFeedDetailResponse(userId, feed));
        }

        // 응답 반환
        return FeedListResponse.builder()
                .feeds(feedResponses)
                .hasNext(feeds.hasNext())
                .build();
    }

    // 피드 수정
    @Transactional
    public FeedUpdateResponse updateFeed(Long userId, Music music, String content, Long feedId) {

        // 로그인한 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 피드가 존재하는지 조회
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        // 로그인한 사용자와 피드 작성자가 일치하는지 조회
        if (!userId.equals(feed.getUser().getId())) {
            log.warn("[FeedService] 피드 수정은 피드 작성자만 수정할 수 있습니다.");
            throw new CustomException(FeedErrorCode.FEED_ACCESS_DENIED);
        }

        // 피드 수정
        feed.updateFeed(music, content);
        Music updatedMusic = feed.getMusic();

        // DB 저장
        feedRepository.save(feed);

        // 로그 출력
        log.info("[FeedService] 피드 수정 성공: feedId = {}", feed.getId());

        // 응답 세팅
        return FeedUpdateResponse.builder()
                .feedId(feed.getId())
                .nickname(user.getNickname())
                .music(MusicResponse.builder()
                        .musicId(updatedMusic.getId())
                        .title(updatedMusic.getTitle())
                        .artist(updatedMusic.getArtist())
                        .musicPicture(updatedMusic.getMusicPicture())
                        .build())
                .content(feed.getContent())
                .createdAt(feed.getCreatedAt())
                .build();
    }

    // 피드 삭제
    @Transactional
    public void deleteFeed(Long userId, Long feedId) {

        // 로그인한 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 피드가 존재하는지 조회
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        // 로그인한 사용자와 작성자가 일치하는지 조회
        if (!userId.equals(feed.getUser().getId())) {
            log.warn("[FeedService] 피드 접근 권한이 없습니다.");
            throw new CustomException(FeedErrorCode.FEED_ACCESS_DENIED);
        }

        // 피드에 달린 메시지의 알림 삭제 (메시지가 지워지면 알림창에 보여줄 내용이 없어지기 때문)
        List<Long> messageIds = new ArrayList<>();
        for (FeedMessage feedMessage : feedMessageRepository.findAllByFeedId(feedId)) {
            messageIds.add(feedMessage.getId());
        }
        if (!messageIds.isEmpty()) {
            notificationRepository.deleteAllByTypeAndTargetIdIn(NotificationType.FEED_MESSAGE, messageIds);
        }

        // 피드 메시지 먼저 삭제
        feedMessageRepository.deleteAllByFeedId(feedId);

        // 피드 삭제
        feedRepository.deleteById(feedId);

        // 로그 출력
        log.info("[FeedService] 피드 삭제 성공: feedId = {}", feedId);
    }

    // 피드 메시지 작성
    @Transactional
    public FeedSendMessageResponse sendMessage(Long userId, Long feedId, String messageContent, Music music) {

        // 로그인한 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 피드가 존재하는지 조회
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        // 본인의 피드인지 확인
        if (userId.equals(feed.getUser().getId())) {
            log.warn("[FeedService] 본인 피드에는 메시지를 보낼 수 없습니다.");
            throw new CustomException(FeedErrorCode.FEED_MESSAGE_SELF);
        }

        // DB 저장
        FeedMessage feedMessage = feedMessageRepository.save(FeedMessage.builder()
                .user(user)
                .feed(feed)
                .music(music)
                .messageContent(messageContent)
                .build());

        // 알림 저장
        notificationRepository.save(Notification.builder()
                .receiver(feed.getUser())
                .sender(user)
                .type(NotificationType.FEED_MESSAGE)
                .targetId(feedMessage.getId())
                .build());

        // 피드 작성자에게 푸시 알림 (커밋 후 전송, 알림을 누르면 피드 상세로 이동)
        eventPublisher.publishEvent(new PushEvent(feed.getUser().getId(), "내 피드에 메시지가 왔어요", user.getNickname() + "님이 메시지를 보냈어요", NotificationType.FEED_MESSAGE, feed.getId()));

        // 로그 출력
        log.info("[FeedService] 피드 메시지 작성 성공: feedMessageId = {}", feedMessage.getId());

        // 응답 세팅
        return FeedSendMessageResponse.builder()
                .feedMessageId(feedMessage.getId())
                .feedId(feed.getId())
                .nickname(user.getNickname())
                .profilePicture(user.getProfilePicture())
                .music(MusicResponse.builder()
                        .musicId(music.getId())
                        .title(music.getTitle())
                        .artist(music.getArtist())
                        .musicPicture(music.getMusicPicture())
                        .build())
                .messageContent(messageContent)
                .createdAt(feedMessage.getCreatedAt())
                .build();
    }

    // 보낸 코멘트 리스트 보기
    public SentFeedMessageListResponse getSentMessages(Long userId, Long cursor, int size) {

        Pageable pageable = PageRequest.of(0, size);
        Slice<FeedMessage> feedMessages;

        // 커서가 없으면 내가 보낸 최신 메시지부터 조회
        if (cursor == null) {
            feedMessages = feedMessageRepository.findByUserIdAndDeletedBySenderFalseOrderByIdDesc(userId, pageable);
        }

        // 커서가 있으면 커서보다 이전 메시지 조회
        else {
            feedMessages = feedMessageRepository.findByUserIdAndDeletedBySenderFalseAndIdLessThanOrderByIdDesc(userId, cursor, pageable);
        }

        // 응답 목록 세팅
        List<SentFeedMessageResponse> messageResponses = new ArrayList<>();
        for (FeedMessage feedMessage : feedMessages.getContent()) {
            messageResponses.add(toSentFeedMessageResponse(feedMessage));
        }

        // 응답 반환
        return SentFeedMessageListResponse.builder()
                .messages(messageResponses)
                .hasNext(feedMessages.hasNext())
                .build();
    }

    // 보낸 메시지 엔티티를 응답 dto로 변환
    private SentFeedMessageResponse toSentFeedMessageResponse(FeedMessage feedMessage) {

        Feed feed = feedMessage.getFeed(); // 메시지를 보낸 피드
        User receiver = feed.getUser(); // 받은 사람 (피드 작성자)
        Music music = feedMessage.getMusic(); // 메시지와 함께 보낸 노래

        // 피드 작성자가 탈퇴하면 피드와 메시지가 함께 삭제되기 때문에 탈퇴한 사용자 처리는 하지 않음
        return SentFeedMessageResponse.builder()
                .feedMessageId(feedMessage.getId())
                .feedId(feed.getId())
                .receiverId(receiver.getId())
                .receiverNickname(receiver.getNickname())
                .music(MusicResponse.builder()
                        .musicId(music.getId())
                        .title(music.getTitle())
                        .artist(music.getArtist())
                        .musicPicture(music.getMusicPicture())
                        .build())
                .messageContent(feedMessage.getMessageContent())
                .createdAt(feedMessage.getCreatedAt())
                .build();
    }

    // 보낸 코멘트 전체 삭제
    @Transactional
    public void deleteAllSentMessages(Long userId) {

        // 내가 보내고 아직 보낸 목록에서 삭제하지 않은 메시지 조회
        List<FeedMessage> feedMessages = feedMessageRepository.findAllByUserIdAndDeletedBySenderFalse(userId);

        // 하나씩 보낸 목록에서 삭제 처리 (받은 사람의 알림에는 그대로 남음)
        for (FeedMessage feedMessage : feedMessages) {
            feedMessage.deleteBySender();
        }

        // 로그 출력
        log.info("[FeedService] 보낸 코멘트 전체 삭제 성공: userId = {}, count = {}", userId, feedMessages.size());
    }
}
