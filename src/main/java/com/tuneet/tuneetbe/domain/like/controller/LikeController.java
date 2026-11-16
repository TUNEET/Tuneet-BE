package com.tuneet.tuneetbe.domain.like.controller;

import com.tuneet.tuneetbe.domain.like.dto.response.LikedArtistListResponse;
import com.tuneet.tuneetbe.domain.like.dto.response.LikedMusicListResponse;
import com.tuneet.tuneetbe.domain.like.service.LikeService;
import com.tuneet.tuneetbe.global.common.BaseResponse;
import com.tuneet.tuneetbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/likes")
@Tag(name = "Like", description = "곡·아티스트 좋아요 API")
public class LikeController {

    private static final String MBID = "^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$";

    private final LikeService likeService;

    // 곡 좋아요
    @Operation(summary = "곡 좋아요", description = "곡에 좋아요를 누르는 API. 이미 좋아요한 곡이면 그대로 성공. music-id는 곡 MBID")
    @PutMapping("/musics/{music-id}")
    public ResponseEntity<BaseResponse<Void>> likeMusic(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("music-id")
            @Pattern(regexp = MBID, message = "올바른 노래 id가 아닙니다.")
            String musicId) {

        // service 호출
        likeService.likeMusic(userDetails.getUserId(), musicId);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "곡 좋아요 성공", null));
    }

    // 곡 좋아요 취소
    @Operation(summary = "곡 좋아요 취소",
            description = "곡 좋아요를 취소하는 API. 같은 곡의 다른 음원, 유튜브 뮤직에서 불러온 좋아요도 함께 취소 "
                    + "(불러온 좋아요는 유튜브에서도 좋아요 상태면 다음 동기화 때 다시 들어옴). 좋아요하지 않은 곡이어도 성공")
    @DeleteMapping("/musics/{music-id}")
    public ResponseEntity<BaseResponse<Void>> unlikeMusic(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("music-id")
            @Pattern(regexp = MBID, message = "올바른 노래 id가 아닙니다.")
            String musicId) {

        // service 호출
        likeService.unlikeMusic(userDetails.getUserId(), musicId);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "곡 좋아요 취소 성공", null));
    }

    // 아티스트 좋아요
    @Operation(summary = "아티스트 좋아요", description = "아티스트에 좋아요를 누르는 API. 이미 좋아요한 아티스트면 그대로 성공. artist-id는 아티스트 MBID")
    @PutMapping("/artists/{artist-id}")
    public ResponseEntity<BaseResponse<Void>> likeArtist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("artist-id")
            @Pattern(regexp = MBID, message = "올바른 아티스트 id가 아닙니다.")
            String artistId) {

        // service 호출
        likeService.likeArtist(userDetails.getUserId(), artistId);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "아티스트 좋아요 성공", null));
    }

    // 아티스트 좋아요 취소
    @Operation(summary = "아티스트 좋아요 취소", description = "아티스트 좋아요를 취소하는 API. 좋아요하지 않은 아티스트여도 성공")
    @DeleteMapping("/artists/{artist-id}")
    public ResponseEntity<BaseResponse<Void>> unlikeArtist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("artist-id")
            @Pattern(regexp = MBID, message = "올바른 아티스트 id가 아닙니다.")
            String artistId) {

        // service 호출
        likeService.unlikeArtist(userDetails.getUserId(), artistId);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "아티스트 좋아요 취소 성공", null));
    }

    // 좋아요한 곡 목록
    @Operation(summary = "좋아요한 곡 목록",
            description = "앱에서 누른 좋아요와 유튜브 뮤직에서 불러온 좋아요를 합쳐 최근 좋아요한 순으로 조회하는 API (같은 곡은 한 번). "
                    + "다음 페이지는 마지막 항목의 likeId를 cursor로 보냄")
    @GetMapping("/musics")
    public ResponseEntity<BaseResponse<LikedMusicListResponse>> getLikedMusics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {

        // service 호출
        LikedMusicListResponse response = likeService.getLikedMusics(userDetails.getUserId(), cursor, size);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "좋아요한 곡 목록 조회 성공", response));
    }

    // 좋아요한 아티스트 목록
    @Operation(summary = "좋아요한 아티스트 목록",
            description = "좋아요한 아티스트를 최근 좋아요한 순으로 조회하는 API. 다음 페이지는 마지막 항목의 likeId를 cursor로 보냄")
    @GetMapping("/artists")
    public ResponseEntity<BaseResponse<LikedArtistListResponse>> getLikedArtists(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {

        // service 호출
        LikedArtistListResponse response = likeService.getLikedArtists(userDetails.getUserId(), cursor, size);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "좋아요한 아티스트 목록 조회 성공", response));
    }
}
