package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // 이메일이 존재하는지 확인
    boolean existsByEmail(String email);

    // 닉네임이 존재하는지 확인
    boolean existsByNickname(String nickname);

    // 행 잠금을 걸고 사용자 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    // 랜덤 편지를 받을 수 있는 사용자 수 (나 제외, 탈퇴하지 않음, 편지 받기 켬)
    long countByIdNotAndDeletedAtIsNullAndLetterReceivableTrue(Long userId);

    // 랜덤 편지를 받을 수 있는 사용자 조회 (나 제외, 탈퇴하지 않음, 편지 받기 켬)
    List<User> findByIdNotAndDeletedAtIsNullAndLetterReceivableTrueOrderByIdAsc(Long userId, Pageable pageable);
}
