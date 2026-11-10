package com.tuneet.tuneetbe.domain.letter.service;

import com.tuneet.tuneetbe.domain.letter.dto.response.LetterDetailResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.LetterListResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.LetterSendResponse;
import com.tuneet.tuneetbe.domain.letter.dto.response.RandomLetterSendResponse;
import com.tuneet.tuneetbe.domain.letter.entity.Letter;
import com.tuneet.tuneetbe.domain.letter.exception.LetterErrorCode;
import com.tuneet.tuneetbe.domain.letter.repository.LetterRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LetterServiceTest {

    private static final Long SENDER_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final Long LETTER_ID = 100L;
    private static final String CONTENT = "오늘도 충분히 예쁘고 멋진 하루 보내길 바라";

    @Mock
    private LetterRepository letterRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LetterService letterService;

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
        ReflectionTestUtils.setField(music, "title", "Pretty Girl");
        ReflectionTestUtils.setField(music, "artist", "RESCENE");
        ReflectionTestUtils.setField(music, "musicPicture", "cover.png");
        return music;
    }

    private void givenSentToday(long count) {
        given(letterRepository.countBySenderIdAndCreatedAtGreaterThanEqual(SENDER_ID, LocalDate.now().atStartOfDay()))
                .willReturn(count);
    }

    private void givenSaveReturnsLetterWithId() {
        given(letterRepository.save(any(Letter.class))).willAnswer(invocation -> {
            Letter letter = invocation.getArgument(0);
            ReflectionTestUtils.setField(letter, "id", LETTER_ID);
            return letter;
        });
    }

    @Nested
    @DisplayName("지정 편지 발송")
    class SendLetter {

        @Test
        @DisplayName("받는 사람에게 편지를 저장하고 응답을 반환한다")
        void success() {
            // given
            User sender = savedUser(SENDER_ID, "튜니");
            User receiver = savedUser(RECEIVER_ID, "원이");
            Music music = savedMusic();
            given(userRepository.findByIdForUpdate(SENDER_ID)).willReturn(Optional.of(sender));
            given(userRepository.findById(RECEIVER_ID)).willReturn(Optional.of(receiver));
            givenSentToday(4);
            givenSaveReturnsLetterWithId();

            // when
            LetterSendResponse response = letterService.sendLetter(SENDER_ID, RECEIVER_ID, music, CONTENT);

            // then
            ArgumentCaptor<Letter> captor = ArgumentCaptor.forClass(Letter.class);
            verify(letterRepository).save(captor.capture());
            Letter letter = captor.getValue();
            assertThat(letter.getSender()).isEqualTo(sender);
            assertThat(letter.getReceiver()).isEqualTo(receiver);
            assertThat(letter.getMusic()).isEqualTo(music);
            assertThat(letter.getLetterContent()).isEqualTo(CONTENT);
            assertThat(letter.getIsRandom()).isFalse();
            assertThat(letter.getDeletedByReceiver()).isFalse();

            assertThat(response.getLetterId()).isEqualTo(LETTER_ID);
            assertThat(response.getSenderNickname()).isEqualTo("튜니");
            assertThat(response.getReceiverNickname()).isEqualTo("원이");
            assertThat(response.getMusic().getTitle()).isEqualTo("Pretty Girl");
            assertThat(response.getLetterContent()).isEqualTo(CONTENT);
        }

        @Test
        @DisplayName("나에게 보내면 LETTER_SELF 예외가 발생하고 저장하지 않는다")
        void self() {
            // given
            given(userRepository.findByIdForUpdate(SENDER_ID)).willReturn(Optional.of(savedUser(SENDER_ID, "튜니")));

            // when & then
            assertThatThrownBy(() -> letterService.sendLetter(SENDER_ID, SENDER_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(LetterErrorCode.LETTER_SELF);

            verify(letterRepository, never()).save(any());
        }

        @Test
        @DisplayName("받는 사람이 없으면 USER_NOT_FOUND 예외가 발생한다")
        void receiverNotFound() {
            // given
            given(userRepository.findByIdForUpdate(SENDER_ID)).willReturn(Optional.of(savedUser(SENDER_ID, "튜니")));
            given(userRepository.findById(RECEIVER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> letterService.sendLetter(SENDER_ID, RECEIVER_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);

            verify(letterRepository, never()).save(any());
        }

        @Test
        @DisplayName("받는 사람이 탈퇴했으면 USER_NOT_FOUND 예외가 발생한다")
        void receiverDeleted() {
            // given
            User receiver = savedUser(RECEIVER_ID, "원이");
            receiver.deleteUser(LocalDateTime.now());
            given(userRepository.findByIdForUpdate(SENDER_ID)).willReturn(Optional.of(savedUser(SENDER_ID, "튜니")));
            given(userRepository.findById(RECEIVER_ID)).willReturn(Optional.of(receiver));

            // when & then
            assertThatThrownBy(() -> letterService.sendLetter(SENDER_ID, RECEIVER_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);

            verify(letterRepository, never()).save(any());
        }

        @Test
        @DisplayName("받는 사람이 편지 받기를 껐으면 LETTER_NOT_RECEIVABLE 예외가 발생한다")
        void notReceivable() {
            // given
            User receiver = savedUser(RECEIVER_ID, "원이");
            receiver.updateSettings(null, false);
            given(userRepository.findByIdForUpdate(SENDER_ID)).willReturn(Optional.of(savedUser(SENDER_ID, "튜니")));
            given(userRepository.findById(RECEIVER_ID)).willReturn(Optional.of(receiver));

            // when & then
            assertThatThrownBy(() -> letterService.sendLetter(SENDER_ID, RECEIVER_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(LetterErrorCode.LETTER_NOT_RECEIVABLE);

            verify(letterRepository, never()).save(any());
        }

        @Test
        @DisplayName("오늘 5장을 이미 보냈으면 LETTER_DAILY_LIMIT_EXCEEDED 예외가 발생한다")
        void dailyLimitExceeded() {
            // given
            given(userRepository.findByIdForUpdate(SENDER_ID)).willReturn(Optional.of(savedUser(SENDER_ID, "튜니")));
            given(userRepository.findById(RECEIVER_ID)).willReturn(Optional.of(savedUser(RECEIVER_ID, "원이")));
            givenSentToday(5);

            // when & then
            assertThatThrownBy(() -> letterService.sendLetter(SENDER_ID, RECEIVER_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(LetterErrorCode.LETTER_DAILY_LIMIT_EXCEEDED);

            verify(letterRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("랜덤 편지 발송")
    class SendRandomLetter {

        @Test
        @DisplayName("후보 중 한 명에게 랜덤 편지로 저장하고 응답에는 받는 사람을 포함하지 않는다")
        void success() {
            // given
            User sender = savedUser(SENDER_ID, "튜니");
            User receiver = savedUser(RECEIVER_ID, "원이");
            given(userRepository.findByIdForUpdate(SENDER_ID)).willReturn(Optional.of(sender));
            givenSentToday(0);
            given(userRepository.countByIdNotAndDeletedAtIsNullAndLetterReceivableTrue(SENDER_ID)).willReturn(1L);
            given(userRepository.findByIdNotAndDeletedAtIsNullAndLetterReceivableTrueOrderByIdAsc(SENDER_ID, PageRequest.of(0, 1)))
                    .willReturn(List.of(receiver));
            givenSaveReturnsLetterWithId();

            // when
            RandomLetterSendResponse response = letterService.sendRandomLetter(SENDER_ID, savedMusic(), CONTENT);

            // then
            ArgumentCaptor<Letter> captor = ArgumentCaptor.forClass(Letter.class);
            verify(letterRepository).save(captor.capture());
            assertThat(captor.getValue().getSender()).isEqualTo(sender);
            assertThat(captor.getValue().getReceiver()).isEqualTo(receiver);
            assertThat(captor.getValue().getIsRandom()).isTrue();

            assertThat(response.getLetterId()).isEqualTo(LETTER_ID);
            assertThat(response.getSenderNickname()).isEqualTo("튜니");
            assertThat(response.getLetterContent()).isEqualTo(CONTENT);
        }

        @Test
        @DisplayName("후보가 여러 명이면 후보 수 범위 안의 위치에서 한 명을 조회한다")
        void randomIndexInRange() {
            // given
            given(userRepository.findByIdForUpdate(SENDER_ID)).willReturn(Optional.of(savedUser(SENDER_ID, "튜니")));
            givenSentToday(0);
            given(userRepository.countByIdNotAndDeletedAtIsNullAndLetterReceivableTrue(SENDER_ID)).willReturn(3L);
            given(userRepository.findByIdNotAndDeletedAtIsNullAndLetterReceivableTrueOrderByIdAsc(eq(SENDER_ID), any(Pageable.class)))
                    .willReturn(List.of(savedUser(RECEIVER_ID, "원이")));
            givenSaveReturnsLetterWithId();

            // when
            letterService.sendRandomLetter(SENDER_ID, savedMusic(), CONTENT);

            // then
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(userRepository).findByIdNotAndDeletedAtIsNullAndLetterReceivableTrueOrderByIdAsc(eq(SENDER_ID), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(1);
            assertThat(captor.getValue().getPageNumber()).isBetween(0, 2);
        }

        @Test
        @DisplayName("받을 수 있는 사용자가 없으면 LETTER_NO_RANDOM_RECEIVER 예외가 발생하고 저장하지 않는다")
        void noReceiver() {
            // given
            given(userRepository.findByIdForUpdate(SENDER_ID)).willReturn(Optional.of(savedUser(SENDER_ID, "튜니")));
            givenSentToday(0);
            given(userRepository.countByIdNotAndDeletedAtIsNullAndLetterReceivableTrue(SENDER_ID)).willReturn(0L);

            // when & then
            assertThatThrownBy(() -> letterService.sendRandomLetter(SENDER_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(LetterErrorCode.LETTER_NO_RANDOM_RECEIVER);

            verify(letterRepository, never()).save(any());
        }

        @Test
        @DisplayName("후보 수를 센 뒤 후보가 줄어 조회 결과가 비면 LETTER_NO_RANDOM_RECEIVER 예외가 발생하고 저장하지 않는다")
        void candidateRemovedAfterCount() {
            // given
            given(userRepository.findByIdForUpdate(SENDER_ID)).willReturn(Optional.of(savedUser(SENDER_ID, "튜니")));
            givenSentToday(0);
            given(userRepository.countByIdNotAndDeletedAtIsNullAndLetterReceivableTrue(SENDER_ID)).willReturn(1L);
            given(userRepository.findByIdNotAndDeletedAtIsNullAndLetterReceivableTrueOrderByIdAsc(SENDER_ID, PageRequest.of(0, 1)))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> letterService.sendRandomLetter(SENDER_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(LetterErrorCode.LETTER_NO_RANDOM_RECEIVER);

            verify(letterRepository, never()).save(any());
        }

        @Test
        @DisplayName("오늘 5장을 이미 보냈으면 받는 사람을 고르지 않고 LETTER_DAILY_LIMIT_EXCEEDED 예외가 발생한다")
        void dailyLimitExceeded() {
            // given
            given(userRepository.findByIdForUpdate(SENDER_ID)).willReturn(Optional.of(savedUser(SENDER_ID, "튜니")));
            givenSentToday(5);

            // when & then
            assertThatThrownBy(() -> letterService.sendRandomLetter(SENDER_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(LetterErrorCode.LETTER_DAILY_LIMIT_EXCEEDED);

            verify(userRepository, never()).countByIdNotAndDeletedAtIsNullAndLetterReceivableTrue(anyLong());
            verify(letterRepository, never()).save(any());
        }

        @Test
        @DisplayName("보내는 사람이 없으면 USER_NOT_FOUND 예외가 발생한다")
        void senderNotFound() {
            // given
            given(userRepository.findByIdForUpdate(SENDER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> letterService.sendRandomLetter(SENDER_ID, savedMusic(), CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }
    }

    private Letter savedLetter(Long id, User sender, User receiver) {
        Letter letter = Letter.builder()
                .sender(sender)
                .receiver(receiver)
                .music(savedMusic())
                .letterContent(CONTENT)
                .isRandom(false)
                .build();
        ReflectionTestUtils.setField(letter, "id", id);
        return letter;
    }

    @Nested
    @DisplayName("편지 상세보기")
    class LetterDetail {

        @Test
        @DisplayName("내가 받은 편지면 보낸 사람 정보를 반환하고 읽음 처리한다")
        void success() {
            // given
            Letter letter = savedLetter(LETTER_ID, savedUser(SENDER_ID, "튜니"), savedUser(RECEIVER_ID, "원이"));
            given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));

            // when
            LetterDetailResponse response = letterService.letterDetail(RECEIVER_ID, LETTER_ID);

            // then
            assertThat(letter.getReadAt()).isNotNull();
            assertThat(response.getLetterId()).isEqualTo(LETTER_ID);
            assertThat(response.getSenderId()).isEqualTo(SENDER_ID);
            assertThat(response.getSenderNickname()).isEqualTo("튜니");
            assertThat(response.getMusic().getTitle()).isEqualTo("Pretty Girl");
            assertThat(response.getLetterContent()).isEqualTo(CONTENT);
            assertThat(response.getIsRead()).isTrue();
        }

        @Test
        @DisplayName("이미 읽은 편지는 처음 읽은 시간을 유지한다")
        void keepFirstReadAt() {
            // given
            Letter letter = savedLetter(LETTER_ID, savedUser(SENDER_ID, "튜니"), savedUser(RECEIVER_ID, "원이"));
            LocalDateTime firstReadAt = LocalDateTime.of(2026, 11, 1, 9, 0);
            ReflectionTestUtils.setField(letter, "readAt", firstReadAt);
            given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));

            // when
            letterService.letterDetail(RECEIVER_ID, LETTER_ID);

            // then
            assertThat(letter.getReadAt()).isEqualTo(firstReadAt);
        }

        @Test
        @DisplayName("탈퇴한 사용자가 보낸 편지는 보낸 사람을 탈퇴한 사용자로 표시한다")
        void deletedSender() {
            // given
            User sender = savedUser(SENDER_ID, "튜니");
            sender.deleteUser(LocalDateTime.now());
            given(letterRepository.findById(LETTER_ID))
                    .willReturn(Optional.of(savedLetter(LETTER_ID, sender, savedUser(RECEIVER_ID, "원이"))));

            // when
            LetterDetailResponse response = letterService.letterDetail(RECEIVER_ID, LETTER_ID);

            // then
            assertThat(response.getSenderNickname()).isEqualTo("탈퇴한 사용자");
        }

        @Test
        @DisplayName("다른 사람이 받은 편지면 LETTER_NOT_FOUND 예외가 발생하고 읽음 처리하지 않는다")
        void otherUserLetter() {
            // given
            Letter letter = savedLetter(LETTER_ID, savedUser(SENDER_ID, "튜니"), savedUser(RECEIVER_ID, "원이"));
            given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));

            // when & then
            assertThatThrownBy(() -> letterService.letterDetail(3L, LETTER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(LetterErrorCode.LETTER_NOT_FOUND);

            assertThat(letter.getReadAt()).isNull();
        }

        @Test
        @DisplayName("내가 삭제한 편지면 LETTER_NOT_FOUND 예외가 발생한다")
        void deletedByReceiver() {
            // given
            Letter letter = savedLetter(LETTER_ID, savedUser(SENDER_ID, "튜니"), savedUser(RECEIVER_ID, "원이"));
            ReflectionTestUtils.setField(letter, "deletedByReceiver", true);
            given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));

            // when & then
            assertThatThrownBy(() -> letterService.letterDetail(RECEIVER_ID, LETTER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(LetterErrorCode.LETTER_NOT_FOUND);
        }

        @Test
        @DisplayName("편지가 없으면 LETTER_NOT_FOUND 예외가 발생한다")
        void letterNotFound() {
            // given
            given(letterRepository.findById(LETTER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> letterService.letterDetail(RECEIVER_ID, LETTER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(LetterErrorCode.LETTER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("편지 리스트 보기")
    class LetterList {

        private static final int SIZE = 2;
        private final PageRequest pageable = PageRequest.of(0, SIZE);

        @Test
        @DisplayName("커서가 없으면 내가 받은 최신 편지부터 조회하고 읽음 여부와 다음 편지 여부를 반환한다")
        void firstPage() {
            // given
            User receiver = savedUser(RECEIVER_ID, "원이");
            Letter unread = savedLetter(3L, savedUser(SENDER_ID, "튜니"), receiver);
            Letter read = savedLetter(2L, savedUser(SENDER_ID, "튜니"), receiver);
            ReflectionTestUtils.setField(read, "readAt", LocalDateTime.now());
            given(letterRepository.findByReceiverIdAndDeletedByReceiverFalseOrderByIdDesc(RECEIVER_ID, pageable))
                    .willReturn(new SliceImpl<>(List.of(unread, read), pageable, true));

            // when
            LetterListResponse response = letterService.letterList(RECEIVER_ID, null, SIZE);

            // then
            assertThat(response.getLetters()).extracting("letterId").containsExactly(3L, 2L);
            assertThat(response.getLetters()).extracting("isRead").containsExactly(false, true);
            assertThat(response.getHasNext()).isTrue();
            verify(letterRepository, never())
                    .findByReceiverIdAndDeletedByReceiverFalseAndIdLessThanOrderByIdDesc(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("커서가 있으면 커서보다 이전 편지를 조회한다")
        void nextPage() {
            // given
            Letter letter = savedLetter(1L, savedUser(SENDER_ID, "튜니"), savedUser(RECEIVER_ID, "원이"));
            given(letterRepository.findByReceiverIdAndDeletedByReceiverFalseAndIdLessThanOrderByIdDesc(RECEIVER_ID, 2L, pageable))
                    .willReturn(new SliceImpl<>(List.of(letter), pageable, false));

            // when
            LetterListResponse response = letterService.letterList(RECEIVER_ID, 2L, SIZE);

            // then
            assertThat(response.getLetters()).extracting("letterId").containsExactly(1L);
            assertThat(response.getHasNext()).isFalse();
            verify(letterRepository, never()).findByReceiverIdAndDeletedByReceiverFalseOrderByIdDesc(anyLong(), any());
        }

        @Test
        @DisplayName("받은 편지가 없으면 빈 목록과 hasNext false를 반환한다")
        void empty() {
            // given
            given(letterRepository.findByReceiverIdAndDeletedByReceiverFalseOrderByIdDesc(RECEIVER_ID, pageable))
                    .willReturn(new SliceImpl<>(List.of(), pageable, false));

            // when
            LetterListResponse response = letterService.letterList(RECEIVER_ID, null, SIZE);

            // then
            assertThat(response.getLetters()).isEmpty();
            assertThat(response.getHasNext()).isFalse();
        }
    }
}
