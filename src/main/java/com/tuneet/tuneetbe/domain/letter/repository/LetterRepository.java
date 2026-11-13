package com.tuneet.tuneetbe.domain.letter.repository;

import com.tuneet.tuneetbe.domain.letter.entity.Letter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LetterRepository extends JpaRepository<Letter, Long> {

    // 보낸 사람이 기준 시각 이후에 보낸 편지 수 (답장은 하루 제한에서 제외)
    long countBySenderIdAndParentLetterIsNullAndCreatedAtGreaterThanEqual(Long senderId, LocalDateTime from);

    // 내가 받은 편지를 최신순으로 보낸 사람과 노래를 함께 조회
    @EntityGraph(attributePaths = {"sender", "music"})
    Slice<Letter> findByReceiverIdAndDeletedByReceiverFalseOrderByIdDesc(Long receiverId, Pageable pageable);

    // 내가 받은 편지 중 커서보다 이전 편지를 보낸 사람과 노래를 함께 조회
    @EntityGraph(attributePaths = {"sender", "music"})
    Slice<Letter> findByReceiverIdAndDeletedByReceiverFalseAndIdLessThanOrderByIdDesc(Long receiverId, Long cursor, Pageable pageable);

    // 내가 받고 아직 삭제하지 않은 편지 전체 조회
    // TODO: 추후 편지가 많아지면 Query 방식으로 변경하는 것이 좋을것 같음
    List<Letter> findAllByReceiverIdAndDeletedByReceiverFalse(Long receiverId);

    // 이 편지에 이미 답장했는지 확인
    boolean existsByParentLetterId(Long parentLetterId);

    // 내가 보낸 편지를 최신순으로 받는 사람과 노래를 함께 조회
    @EntityGraph(attributePaths = {"receiver", "music"})
    Slice<Letter> findBySenderIdAndDeletedBySenderFalseOrderByIdDesc(Long senderId, Pageable pageable);

    // 내가 보낸 편지 중 커서보다 이전 편지를 받는 사람과 노래를 함께 조회
    @EntityGraph(attributePaths = {"receiver", "music"})
    Slice<Letter> findBySenderIdAndDeletedBySenderFalseAndIdLessThanOrderByIdDesc(Long senderId, Long cursor, Pageable pageable);

    // 내가 보내고 아직 보낸 목록에서 삭제하지 않은 편지 전체 조회
    List<Letter> findAllBySenderIdAndDeletedBySenderFalse(Long senderId);
}
