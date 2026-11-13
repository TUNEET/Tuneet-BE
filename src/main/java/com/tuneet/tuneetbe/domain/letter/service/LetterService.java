package com.tuneet.tuneetbe.domain.letter.service;

import com.tuneet.tuneetbe.domain.letter.dto.response.LetterDetailResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.LetterListResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.LetterRemainingResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.LetterSendResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.RandomLetterSendResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.SentLetterListResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.SentLetterResponse;
import com.tuneet.tuneetbe.domain.letter.entity.Letter;
import com.tuneet.tuneetbe.domain.letter.exception.LetterErrorCode;
import com.tuneet.tuneetbe.domain.letter.repository.LetterRepository;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.notification.enums.NotificationType;
import com.tuneet.tuneetbe.domain.notification.event.PushEvent;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.entity.UserBlock;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserBlockRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LetterService {

    private static final int DAILY_LETTER_LIMIT = 5; // 하루에 편지 보낼 수 있는 횟수
    private static final String DELETED_USER_NICKNAME = "탈퇴한 사용자"; // 탈퇴한 사용자 표시 닉네임

    private final LetterRepository letterRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserBlockRepository userBlockRepository;

    // 지정 편지 발송
    @Transactional
    public LetterSendResponse sendLetter(Long userId, Long receiverId, Music music, String letterContent) {

        // 보내는 사람 조회
        // findById로 조회하면 동시 요청 시 오늘 보낸 편지 수를 같이 읽어 하루 5장을 넘을 수 있기 때문에 findByIdForUpdate로 사용자 행을 잠가 요청을 순서대로 처리
        User sender = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 나 자신에게 보내는지 확인
        if (userId.equals(receiverId)) {
            log.warn("[LetterService] 자기 자신에게는 편지를 보낼 수 없습니다.");
            throw new CustomException(LetterErrorCode.LETTER_SELF);
        }

        // 받는 사람 조회
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 사용자가 탈퇴했는지 확인
        if (receiver.isDeleted()) {
            log.warn("[LetterService] 탈퇴한 사용자에게는 편지를 보낼 수 없습니다.");
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }

        // 차단 관계인 사용자인지 확인 (내가 차단했거나 상대가 나를 차단하면 편지를 보낼 수 없음)
        if (userBlockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(userId, receiverId, receiverId, userId)) {
            log.warn("[LetterService] 차단 관계인 사용자에게는 편지를 보낼 수 없습니다.");
            throw new CustomException(UserErrorCode.BLOCKED_USER);
        }

        // 받는 사람의 편지 받기 설정 확인
        if (!receiver.getLetterReceivable()) {
            log.warn("[LetterService] 편지 수신 거부 사용자에게는 편지를 보낼 수 없습니다.");
            throw new CustomException(LetterErrorCode.LETTER_NOT_RECEIVABLE);
        }

        // 하루 편지 제한 확인
        long sentCount = countTodaySentLetters(userId);
        if (sentCount >= DAILY_LETTER_LIMIT) {
            log.warn("[LetterService] 오늘 보낼 수 있는 편지를 모두 사용했습니다.");
            throw new CustomException(LetterErrorCode.LETTER_DAILY_LIMIT_EXCEEDED);
        }

        // DB 저장
        Letter letter = letterRepository.save(Letter.builder()
                .sender(sender)
                .receiver(receiver)
                .music(music)
                .letterContent(letterContent)
                .isRandom(false)
                .build());

        // 받는 사람에게 푸시 알림 (커밋 후 전송)
        eventPublisher.publishEvent(new PushEvent(receiver.getId(), "새 편지가 도착했어요", sender.getNickname() + "님이 편지를 보냈어요", NotificationType.LETTER, letter.getId()));

        // 로그 출력
        log.info("[LetterService] 편지 발송 성공: letterId = {}", letter.getId());

        // 응답 반환
        return LetterSendResponse.builder()
                .letterId(letter.getId())
                .senderNickname(sender.getNickname())
                .music(MusicResponse.builder()
                        .musicId(music.getExternalId())
                        .title(music.getTitle())
                        .artist(music.getArtist())
                        .musicPicture(music.getMusicPicture())
                        .build())
                .letterContent(letter.getLetterContent())
                .receiverNickname(receiver.getNickname())
                .createdAt(letter.getCreatedAt())
                .build();
    }

    // 랜덤 편지 발송
    @Transactional
    public RandomLetterSendResponse sendRandomLetter(Long userId, Music music, String letterContent) {

        // 보내는 사람 조회
        // findById로 조회하면 동시 요청 시 오늘 보낸 편지 수를 같이 읽어 하루 5장을 넘을 수 있기 때문에 findByIdForUpdate로 사용자 행을 잠가 요청을 순서대로 처리
        User sender = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 하루 편지 제한 확인 (받는 사람을 고르기 전에 먼저 확인)
        long sentCount = countTodaySentLetters(userId);
        if (sentCount >= DAILY_LETTER_LIMIT) {
            log.warn("[LetterService] 오늘 보낼 수 있는 편지를 모두 사용했습니다.");
            throw new CustomException(LetterErrorCode.LETTER_DAILY_LIMIT_EXCEEDED);
        }

        // 후보에서 제외할 사용자 (나 + 나와 차단 관계인 사용자)
        List<Long> excludedUserIds = getBlockedRelationUserIds(userId);
        excludedUserIds.add(userId);

        // 받을 수 있는 사용자 수 확인 (나, 탈퇴한 사용자, 편지 받기를 끈 사용자 제외)
        long candidateCount = userRepository.countByIdNotInAndDeletedAtIsNullAndLetterReceivableTrue(excludedUserIds);
        if (candidateCount == 0) {
            log.warn("[LetterService] 랜덤 편지를 받을 수 있는 사용자가 없습니다.");
            throw new CustomException(LetterErrorCode.LETTER_NO_RANDOM_RECEIVER);
        }

        // 후보 중 랜덤 위치 선택 (0 ~ 후보 수 - 1)
        // new Random()을 요청마다 만들지 않도록, 스레드마다 하나씩 재사용하는 ThreadLocalRandom 사용
        int randomIndex = ThreadLocalRandom.current().nextInt((int) candidateCount);

        // 랜덤 위치의 사용자 한 명 조회 (한 명씩 나눈 페이지 중 randomIndex 번째 페이지)
        List<User> candidates = userRepository.findByIdNotInAndDeletedAtIsNullAndLetterReceivableTrueOrderByIdAsc(excludedUserIds, PageRequest.of(randomIndex, 1));

        // 그 사이 후보가 편지 수신 거부를 하거나 탈퇴하면 조회 결과가 없을 수도 있기 때문에 받을 사람이 없는 것으로 처리
        if (candidates.isEmpty()) {
            log.warn("[LetterService] 랜덤 편지를 받을 수 있는 사용자가 없습니다.");
            throw new CustomException(LetterErrorCode.LETTER_NO_RANDOM_RECEIVER);
        }
        User receiver = candidates.get(0);

        // DB 저장
        Letter letter = letterRepository.save(Letter.builder()
                .sender(sender)
                .receiver(receiver)
                .music(music)
                .letterContent(letterContent)
                .isRandom(true)
                .build());

        // 받는 사람에게 푸시 알림 (커밋 후 전송)
        eventPublisher.publishEvent(new PushEvent(receiver.getId(), "새 편지가 도착했어요", sender.getNickname() + "님이 편지를 보냈어요", NotificationType.LETTER, letter.getId()));

        // 로그 출력
        log.info("[LetterService] 랜덤 편지 발송 성공: letterId = {}", letter.getId());

        // 응답 반환 (받는 사람 선택만 랜덤이고, 보낸 사람은 누가 받았는지 알 수 있음)
        return RandomLetterSendResponse.builder()
                .letterId(letter.getId())
                .senderNickname(sender.getNickname())
                .music(MusicResponse.builder()
                        .musicId(music.getExternalId())
                        .title(music.getTitle())
                        .artist(music.getArtist())
                        .musicPicture(music.getMusicPicture())
                        .build())
                .receiverNickname(receiver.getNickname()) // 발송 결과 화면에서 받는 사람 표시
                .letterContent(letter.getLetterContent())
                .createdAt(letter.getCreatedAt())
                .build();
    }

    // 편지 상세보기
    // 읽은 시간을 저장해야 되기 때문에 @Transactional 사용
    @Transactional
    public LetterDetailResponse letterDetail(Long userId, Long letterId) {

        // 편지가 존재하는지 조회
        Letter letter = letterRepository.findById(letterId)
                .orElseThrow(() -> new CustomException(LetterErrorCode.LETTER_NOT_FOUND));

        // 내가 받은 편지인지, 삭제하지 않았는지 확인
        if (!letter.getReceiver().getId().equals(userId) || letter.getDeletedByReceiver()) {
            log.warn("[LetterService] 조회할 수 없는 편지입니다.");
            throw new CustomException(LetterErrorCode.LETTER_NOT_FOUND);
        }

        // 처음 열었을 때 읽음 처리
        letter.markAsRead(LocalDateTime.now());

        // 응답 반환
        // 내가 이 편지에 이미 답장했는지 (답장하기 버튼 표시용)
        boolean isReplied = letterRepository.existsByParentLetterId(letterId);

        // 답장이면 답장 대상 편지(내가 보낸 편지)도 함께 반환 (상대가 편지함에서 삭제했어도 표시)
        return toLetterDetailResponse(letter, toParentLetter(letter.getParentLetter()), isReplied);
    }

    // 편지 리스트 보기
    public LetterListResponse letterList(Long userId, Long cursor, int size) {

        Pageable pageable = PageRequest.of(0, size);
        Slice<Letter> letters;

        // 커서가 없으면 내가 받은 최신 편지부터 조회
        if (cursor == null) {
            letters = letterRepository.findByReceiverIdAndDeletedByReceiverFalseOrderByIdDesc(userId, pageable);
        }

        // 커서가 있으면 커서보다 이전 편지 조회
        else {
            letters = letterRepository.findByReceiverIdAndDeletedByReceiverFalseAndIdLessThanOrderByIdDesc(userId, cursor, pageable);
        }

        // 응답 목록 세팅
        List<LetterDetailResponse> letterResponses = new ArrayList<>();
        for (Letter letter : letters.getContent()) {
            // 목록에서는 답장 대상 편지를 조회하지 않음 (편지마다 추가 조회가 생기는 N+1 방지)
            letterResponses.add(toLetterDetailResponse(letter, null, null));
        }

        // 응답 반환
        return LetterListResponse.builder()
                .letters(letterResponses)
                .hasNext(letters.hasNext())
                .build();
    }

    // Letter 엔티티를 응답 dto로 변환
    private LetterDetailResponse toLetterDetailResponse(Letter letter, LetterDetailResponse.ParentLetter parentLetter, Boolean isReplied) {

        User sender = letter.getSender(); // 보낸 사람
        Music music = letter.getMusic(); // 편지에 담긴 노래

        // 탈퇴한 사용자가 보낸 편지는 "탈퇴한 사용자"로 표시
        String senderNickname;
        if (sender.isDeleted()) {
            senderNickname = DELETED_USER_NICKNAME;
        }
        else {
            senderNickname = sender.getNickname();
        }

        return LetterDetailResponse.builder()
                .letterId(letter.getId())
                .senderId(sender.getId())
                .senderNickname(senderNickname)
                .music(MusicResponse.builder()
                        .musicId(music.getExternalId())
                        .title(music.getTitle())
                        .artist(music.getArtist())
                        .musicPicture(music.getMusicPicture())
                        .build())
                .letterContent(letter.getLetterContent())
                .isRead(letter.getReadAt() != null)
                .isReply(letter.getParentLetter() != null)
                .isReplied(isReplied)
                .parentLetter(parentLetter)
                .createdAt(letter.getCreatedAt())
                .build();
    }

    // 답장 대상 편지를 응답 dto로 변환 (답장이 아니면 null)
    private LetterDetailResponse.ParentLetter toParentLetter(Letter parentLetter) {

        if (parentLetter == null) {
            return null;
        }

        Music music = parentLetter.getMusic(); // 답장 대상 편지에 담긴 노래

        return LetterDetailResponse.ParentLetter.builder()
                .letterId(parentLetter.getId())
                .letterContent(parentLetter.getLetterContent())
                .music(MusicResponse.builder()
                        .musicId(music.getExternalId())
                        .title(music.getTitle())
                        .artist(music.getArtist())
                        .musicPicture(music.getMusicPicture())
                        .build())
                .createdAt(parentLetter.getCreatedAt())
                .build();
    }

    // 편지 단건 삭제
    @Transactional
    public void deleteLetter(Long userId, Long letterId) {

        // 편지가 존재하는지 조회
        Letter letter = letterRepository.findById(letterId)
                .orElseThrow(() -> new CustomException(LetterErrorCode.LETTER_NOT_FOUND));

        // 내가 받은 편지인지, 이미 삭제하지 않았는지 확인
        if (!letter.getReceiver().getId().equals(userId) || letter.getDeletedByReceiver()) {
            log.warn("[LetterService] 삭제할 수 없는 편지입니다.");
            throw new CustomException(LetterErrorCode.LETTER_NOT_FOUND);
        }

        // DB 삭제가 아닌 편지함에서 삭제
        letter.deleteByReceiver();

        // 로그 출력
        log.info("[LetterService] 편지 삭제 성공: letterId = {}", letterId);
    }

    // 편지 전체 삭제
    @Transactional
    public void deleteAllLetters(Long userId) {

        // 내가 받고 아직 삭제하지 않은 편지 조회
        List<Letter> letters = letterRepository.findAllByReceiverIdAndDeletedByReceiverFalse(userId);

        // 하나씩 삭제 처리
        for (Letter letter : letters) {
            letter.deleteByReceiver();
        }

        // 로그 출력
        log.info("[LetterService] 편지 전체 삭제 성공: userId = {}, count = {}", userId, letters.size());
    }

    // 남은 편지 조회
    public LetterRemainingResponse letterRemaining(Long userId) {

        // 오늘 보낸 편지 수
        long sentCount = countTodaySentLetters(userId);

        // 남은 수량 (5장을 넘었어도 0 미만으로 내려가지 않게)
        int remaining = (int) Math.max(0, DAILY_LETTER_LIMIT - sentCount);

        // 응답 반환
        return LetterRemainingResponse.builder()
                .remaining(remaining)
                .limit(DAILY_LETTER_LIMIT)
                .build();
    }

    // 오늘 자정 이후 보낸 편지 수 (답장 제외)
    private long countTodaySentLetters(Long userId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return letterRepository.countBySenderIdAndParentLetterIsNullAndCreatedAtGreaterThanEqual(userId, todayStart);
    }

    // 편지 답장 보내기
    @Transactional
    public LetterSendResponse letterReply(Long userId, Long letterId, Music music, String letterContent) {

        // 보내는 사람 조회
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 답장 대상 편지 조회
        Letter parentLetter = letterRepository.findById(letterId)
                .orElseThrow(() -> new CustomException(LetterErrorCode.LETTER_NOT_FOUND));

        // 내가 받은 편지인지, 삭제하지 않았는지 확인
        if (!parentLetter.getReceiver().getId().equals(userId) || parentLetter.getDeletedByReceiver()) {
            log.warn("[LetterService] 답장할 수 없는 편지입니다.");
            throw new CustomException(LetterErrorCode.LETTER_NOT_FOUND);
        }

        // 이미 답장한 편지인지 확인 (한 편지에는 답장을 한 번만 보낼 수 있음)
        if (letterRepository.existsByParentLetterId(letterId)) {
            log.warn("[LetterService] 이미 답장한 편지입니다.");
            throw new CustomException(LetterErrorCode.LETTER_ALREADY_REPLIED);
        }

        // 받는 사람은 답장 대상 편지를 보낸 사람
        User receiver = parentLetter.getSender();

        // 사용자가 탈퇴했는지 확인
        if (receiver.isDeleted()) {
            log.warn("[LetterService] 탈퇴한 사용자에게는 답장을 보낼 수 없습니다.");
            throw new CustomException(LetterErrorCode.LETTER_REPLY_TO_DELETED_USER);
        }

        // 차단 관계인 사용자인지 확인 (차단 전에 받은 편지라도 답장은 보낼 수 없음)
        if (userBlockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(userId, receiver.getId(), receiver.getId(), userId)) {
            log.warn("[LetterService] 차단 관계인 사용자에게는 답장을 보낼 수 없습니다.");
            throw new CustomException(UserErrorCode.BLOCKED_USER);
        }

        // 받는 사람의 편지 받기 설정 확인
        if (!receiver.getLetterReceivable()) {
            log.warn("[LetterService] 편지 수신 거부 사용자에게는 답장을 보낼 수 없습니다.");
            throw new CustomException(LetterErrorCode.LETTER_NOT_RECEIVABLE);
        }

        // DB 저장
        Letter letter = letterRepository.save(Letter.builder()
                .sender(sender)
                .receiver(receiver)
                .music(music)
                .letterContent(letterContent)
                .isRandom(false)
                .parentLetter(parentLetter)
                .build());

        // 받는 사람에게 푸시 알림 (커밋 후 전송)
        eventPublisher.publishEvent(new PushEvent(receiver.getId(), "답장이 도착했어요", sender.getNickname() + "님이 답장을 보냈어요", NotificationType.LETTER, letter.getId()));

        // 로그 출력
        log.info("[LetterService] 답장 편지 발송 성공: letterId = {}", letter.getId());

        // 응답 반환
        return LetterSendResponse.builder()
                .letterId(letter.getId())
                .senderNickname(sender.getNickname())
                .music(MusicResponse.builder()
                        .musicId(music.getExternalId())
                        .title(music.getTitle())
                        .artist(music.getArtist())
                        .musicPicture(music.getMusicPicture())
                        .build())
                .letterContent(letter.getLetterContent())
                .receiverNickname(receiver.getNickname())
                .createdAt(letter.getCreatedAt())
                .build();
    }

    // 보낸 편지 리스트 보기
    public SentLetterListResponse sentLetterList(Long userId, Long cursor, int size) {

        Pageable pageable = PageRequest.of(0, size);
        Slice<Letter> letters;

        // 커서가 없으면 내가 보낸 최신 편지부터 조회
        if (cursor == null) {
            letters = letterRepository.findBySenderIdAndDeletedBySenderFalseOrderByIdDesc(userId, pageable);
        }

        // 커서가 있으면 커서보다 이전 편지 조회
        else {
            letters = letterRepository.findBySenderIdAndDeletedBySenderFalseAndIdLessThanOrderByIdDesc(userId, cursor, pageable);
        }

        // 응답 목록 세팅
        List<SentLetterResponse> letterResponses = new ArrayList<>();
        for (Letter letter : letters.getContent()) {
            letterResponses.add(toSentLetterResponse(letter));
        }

        // 응답 반환
        return SentLetterListResponse.builder()
                .letters(letterResponses)
                .hasNext(letters.hasNext())
                .build();
    }

    // 보낸 편지 엔티티를 응답 dto로 변환
    private SentLetterResponse toSentLetterResponse(Letter letter) {

        User receiver = letter.getReceiver(); // 받는 사람
        Music music = letter.getMusic(); // 편지에 담긴 노래

        // 받는 사람이 탈퇴했으면 "탈퇴한 사용자"로 표시 (랜덤 편지도 받는 사람 공개)
        String receiverNickname;
        if (receiver.isDeleted()) {
            receiverNickname = DELETED_USER_NICKNAME;
        }
        else {
            receiverNickname = receiver.getNickname();
        }

        return SentLetterResponse.builder()
                .letterId(letter.getId())
                .receiverId(receiver.getId())
                .receiverNickname(receiverNickname)
                .music(MusicResponse.builder()
                        .musicId(music.getExternalId())
                        .title(music.getTitle())
                        .artist(music.getArtist())
                        .musicPicture(music.getMusicPicture())
                        .build())
                .letterContent(letter.getLetterContent())
                .isRandom(letter.getIsRandom())
                .isReply(letter.getParentLetter() != null)
                .createdAt(letter.getCreatedAt())
                .build();
    }

    // 보낸 편지 전체 삭제
    @Transactional
    public void deleteAllSentLetters(Long userId) {

        // 내가 보내고 아직 보낸 목록에서 삭제하지 않은 편지 조회
        List<Letter> letters = letterRepository.findAllBySenderIdAndDeletedBySenderFalse(userId);

        // 하나씩 보낸 목록에서 삭제 처리 (받은 사람의 편지함에는 그대로 남음)
        for (Letter letter : letters) {
            letter.deleteBySender();
        }

        // 로그 출력
        log.info("[LetterService] 보낸 편지 전체 삭제 성공: userId = {}, count = {}", userId, letters.size());
    }

    // 나와 차단 관계인 사용자 id 목록 (내가 차단한 사용자 + 나를 차단한 사용자)
    private List<Long> getBlockedRelationUserIds(Long userId) {

        List<Long> blockedUserIds = new ArrayList<>();
        for (UserBlock userBlock : userBlockRepository.findAllByBlockerIdOrBlockedId(userId, userId)) {

            // 내가 차단한 기록이면 차단된 사람, 나를 차단한 기록이면 차단한 사람
            if (userBlock.getBlocker().getId().equals(userId)) {
                blockedUserIds.add(userBlock.getBlocked().getId());
            }
            else {
                blockedUserIds.add(userBlock.getBlocker().getId());
            }
        }

        return blockedUserIds;
    }
}
