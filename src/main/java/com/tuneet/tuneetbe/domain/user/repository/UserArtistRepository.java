package com.tuneet.tuneetbe.domain.user.repository;

import com.tuneet.tuneetbe.domain.user.entity.UserArtist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserArtistRepository extends JpaRepository<UserArtist, Long> {

    List<UserArtist> findAllByUserId(Long userId);
}
