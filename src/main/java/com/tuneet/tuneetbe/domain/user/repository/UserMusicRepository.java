package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserMusic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserMusicRepository extends JpaRepository<UserMusic, Long> {

    List<UserMusic> findAllByUserId(Long userId);
}
