package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // 이메일이 존재하는지 확인
    boolean existsByEmail(String email);

    // 닉네임이 존재하는지 확인
    boolean existsByNickname(String nickname);
}
