package com.tuneet.tuneetbe.domain.letter.repository;

import com.tuneet.tuneetbe.domain.letter.entity.Letter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface LetterRepository extends JpaRepository<Letter, Long> {

    // 보낸 사람이 기준 시각 이후에 보낸 편지 수
    long countBySenderIdAndCreatedAtGreaterThanEqual(Long senderId, LocalDateTime from);
}
