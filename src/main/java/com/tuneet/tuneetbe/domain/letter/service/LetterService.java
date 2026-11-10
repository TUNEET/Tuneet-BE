package com.tuneet.tuneetbe.domain.letter.service;

import com.tuneet.tuneetbe.domain.letter.dto.response.LetterSendResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.RandomLetterSendResponse;
import com.tuneet.tuneetbe.domain.letter.entity.Letter;
import com.tuneet.tuneetbe.domain.letter.exception.LetterErrorCode;
import com.tuneet.tuneetbe.domain.letter.repository.LetterRepository;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LetterService {

    private static final int DAILY_LETTER_LIMIT = 5; // 하루에 편지 보낼 수 있는 횟수

    private final LetterRepository letterRepository;
    private final UserRepository userRepository;

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

        // 받는 사람의 편지 받기 설정 확인
        if (!receiver.getLetterReceivable()) {
            log.warn("[LetterService] 편지 수신 거부 사용자에게는 편지를 보낼 수 없습니다.");
            throw new CustomException(LetterErrorCode.LETTER_NOT_RECEIVABLE);
        }

        // 하루 편지 제한 확인
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long sentCount = letterRepository.countBySenderIdAndCreatedAtGreaterThanEqual(userId, todayStart);
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

        // 로그 출력
        log.info("[LetterService] 편지 발송 성공: letterId = {}", letter.getId());

        // 응답 반환
        return LetterSendResponse.builder()
                .letterId(letter.getId())
                .senderNickname(sender.getNickname())
                .music(MusicResponse.builder()
                        .musicId(music.getId())
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
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long sentCount = letterRepository.countBySenderIdAndCreatedAtGreaterThanEqual(userId, todayStart);
        if (sentCount >= DAILY_LETTER_LIMIT) {
            log.warn("[LetterService] 오늘 보낼 수 있는 편지를 모두 사용했습니다.");
            throw new CustomException(LetterErrorCode.LETTER_DAILY_LIMIT_EXCEEDED);
        }

        // 받을 수 있는 사용자 수 확인 (나, 탈퇴한 사용자, 편지 받기를 끈 사용자 제외)
        long candidateCount = userRepository.countByIdNotAndDeletedAtIsNullAndLetterReceivableTrue(userId);
        if (candidateCount == 0) {
            log.warn("[LetterService] 랜덤 편지를 받을 수 있는 사용자가 없습니다.");
            throw new CustomException(LetterErrorCode.LETTER_NO_RANDOM_RECEIVER);
        }

        // 후보 중 랜덤 위치 선택 (0 ~ 후보 수 - 1)
        // new Random()을 요청마다 만들지 않도록, 스레드마다 하나씩 재사용하는 ThreadLocalRandom 사용
        int randomIndex = ThreadLocalRandom.current().nextInt((int) candidateCount);

        // 랜덤 위치의 사용자 한 명 조회 (한 명씩 나눈 페이지 중 randomIndex 번째 페이지)
        List<User> candidates = userRepository.findByIdNotAndDeletedAtIsNullAndLetterReceivableTrueOrderByIdAsc(userId, PageRequest.of(randomIndex, 1));

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

        // 로그 출력
        log.info("[LetterService] 랜덤 편지 발송 성공: letterId = {}", letter.getId());

        // 응답 반환 (받는 사람은 숨김)
        return RandomLetterSendResponse.builder()
                .letterId(letter.getId())
                .senderNickname(sender.getNickname())
                .music(MusicResponse.builder()
                        .musicId(music.getId())
                        .title(music.getTitle())
                        .artist(music.getArtist())
                        .musicPicture(music.getMusicPicture())
                        .build())
                .letterContent(letter.getLetterContent())
                .createdAt(letter.getCreatedAt())
                .build();
    }
}
