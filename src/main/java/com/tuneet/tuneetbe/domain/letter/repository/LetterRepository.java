package com.tuneet.tuneetbe.domain.letter.repository;

import com.tuneet.tuneetbe.domain.letter.entity.Letter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface LetterRepository extends JpaRepository<Letter, Long> {

    // 보낸 사람이 기준 시각 이후에 보낸 편지 수
    long countBySenderIdAndCreatedAtGreaterThanEqual(Long senderId, LocalDateTime from);

    // 내가 받은 편지를 최신순으로 보낸 사람과 노래를 함께 조회
    @EntityGraph(attributePaths = {"sender", "music"})
    Slice<Letter> findByReceiverIdAndDeletedByReceiverFalseOrderByIdDesc(Long receiverId, Pageable pageable);

    // 내가 받은 편지 중 커서보다 이전 편지를 보낸 사람과 노래를 함께 조회
    @EntityGraph(attributePaths = {"sender", "music"})
    Slice<Letter> findByReceiverIdAndDeletedByReceiverFalseAndIdLessThanOrderByIdDesc(Long receiverId, Long cursor, Pageable pageable);
}
