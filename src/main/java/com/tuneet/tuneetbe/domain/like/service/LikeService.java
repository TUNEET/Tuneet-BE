package com.tuneet.tuneetbe.domain.like.service;

import com.tuneet.tuneetbe.domain.like.dto.response.LikedArtistListResponse;
import com.tuneet.tuneetbe.domain.like.dto.response.LikedMusicListResponse;
import com.tuneet.tuneetbe.domain.like.entity.UserArtistLike;
import com.tuneet.tuneetbe.domain.like.repository.UserArtistLikeRepository;
import com.tuneet.tuneetbe.domain.music.dto.response.ArtistResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.music.entity.Artist;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.service.MusicCatalogService;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.entity.UserMusicLike;
import com.tuneet.tuneetbe.domain.user.enums.LikeSource;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserMusicLikeRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 곡·아티스트 좋아요
 * 처음 보는 곡·아티스트는 음악 API에서 가져와 저장하므로 트랜잭션 밖에서 끝내고, 좋아요 저장만 짧은 트랜잭션으로 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private final UserRepository userRepository;
    private final UserMusicLikeRepository userMusicLikeRepository;
    private final UserArtistLikeRepository userArtistLikeRepository;
    private final MusicCatalogService musicCatalogService;
    private final TransactionTemplate transactionTemplate;

    // 곡 좋아요 (이미 좋아요했으면 그대로)
    public void likeMusic(Long userId, String musicId) {
        Music music = musicCatalogService.getOrCreateMusics(List.of(musicId)).getFirst();

        transactionTemplate.executeWithoutResult(status -> {
            findUserForUpdate(userId);
            userMusicLikeRepository.insertIfAbsent(userId, music.getId(), LikeSource.IN_APP.name());
        });

        log.info("[LikeService] 곡 좋아요 성공: userId = {}, musicId = {}", userId, musicId);
    }

    // 곡 좋아요 취소: 같은 곡의 다른 음원, 유튜브에서 불러온 좋아요도 함께 취소 (불러온 좋아요는 다음 동기화 때 유튜브 상태를 따름)
    public void unlikeMusic(Long userId, String musicId) {
        transactionTemplate.executeWithoutResult(status -> {
            findUserForUpdate(userId);
            List<Long> likeIds = userMusicLikeRepository.findIdsByUserIdAndSameSongAs(userId, musicId);
            if (!likeIds.isEmpty()) {
                userMusicLikeRepository.deleteAllByIdInBatch(likeIds);
            }
        });

        log.info("[LikeService] 곡 좋아요 취소 성공: userId = {}, musicId = {}", userId, musicId);
    }

    // 아티스트 좋아요 (이미 좋아요했으면 그대로)
    public void likeArtist(Long userId, String artistId) {
        Artist artist = musicCatalogService.getOrCreateArtists(List.of(artistId)).getFirst();

        transactionTemplate.executeWithoutResult(status -> {
            findUserForUpdate(userId);
            userArtistLikeRepository.insertIfAbsent(userId, artist.getId());
        });

        log.info("[LikeService] 아티스트 좋아요 성공: userId = {}, artistId = {}", userId, artistId);
    }

    // 아티스트 좋아요 취소
    public void unlikeArtist(Long userId, String artistId) {
        transactionTemplate.executeWithoutResult(status -> {
            findUserForUpdate(userId);
            List<Long> likeIds = userArtistLikeRepository.findIdsByUserIdAndArtistExternalId(userId, artistId);
            if (!likeIds.isEmpty()) {
                userArtistLikeRepository.deleteAllByIdInBatch(likeIds);
            }
        });

        log.info("[LikeService] 아티스트 좋아요 취소 성공: userId = {}, artistId = {}", userId, artistId);
    }

    // 좋아요한 곡 목록 (앱 + 유튜브, 같은 곡은 한 번)
    public LikedMusicListResponse getLikedMusics(Long userId, Long cursor, int size) {
        Slice<UserMusicLike> likes = userMusicLikeRepository.findLatestPerSong(userId, cursor, PageRequest.of(0, size));

        List<LikedMusicListResponse.LikedMusic> musics = likes.getContent().stream()
                .map(like -> {
                    MusicResponse music = MusicResponse.from(like.getMusic());
                    music.markLiked(true);
                    return LikedMusicListResponse.LikedMusic.builder()
                            .likeId(like.getId())
                            .music(music)
                            .source(like.getSource())
                            .likedAt(like.getCreatedAt())
                            .build();
                })
                .toList();

        return LikedMusicListResponse.builder()
                .musics(musics)
                .hasNext(likes.hasNext())
                .build();
    }

    // 좋아요한 아티스트 목록
    public LikedArtistListResponse getLikedArtists(Long userId, Long cursor, int size) {
        Pageable pageable = PageRequest.of(0, size);
        Slice<UserArtistLike> likes = cursor == null
                ? userArtistLikeRepository.findByUserIdOrderByIdDesc(userId, pageable)
                : userArtistLikeRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, cursor, pageable);

        List<LikedArtistListResponse.LikedArtist> artists = likes.getContent().stream()
                .map(like -> LikedArtistListResponse.LikedArtist.builder()
                        .likeId(like.getId())
                        .artist(ArtistResponse.from(like.getArtist()))
                        .likedAt(like.getCreatedAt())
                        .build())
                .toList();

        return LikedArtistListResponse.builder()
                .artists(artists)
                .hasNext(likes.hasNext())
                .build();
    }

    // 같은 사용자의 중복 요청이 순서대로 처리되도록 사용자 행을 잠그고 조회
    // 음악 API를 기다리는 사이 탈퇴했으면 거부 (파기한 좋아요가 다시 저장되지 않도록)
    private User findUserForUpdate(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        if (user.isDeleted()) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
