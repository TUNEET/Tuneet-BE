package com.tuneet.tuneetbe.domain.music.controller;

import com.tuneet.tuneetbe.domain.music.dto.response.ArtistSearchItemResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.GenreResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicSearchItemResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.SearchPageResponse;
import com.tuneet.tuneetbe.domain.music.service.MusicService;
import com.tuneet.tuneetbe.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Music", description = "장르, 노래, 아티스트 조회 API")
public class MusicController {

    private final MusicService musicService;

    // 장르 조회
    @Operation(summary = "장르 조회", description = "선택 가능한 장르 목록을 화면 순서대로 조회하는 API")
    @GetMapping("/genres")
    public ResponseEntity<BaseResponse<List<GenreResponse>>> getGenres() {
        return ResponseEntity.ok(BaseResponse.success(200, "장르 조회 성공", musicService.getGenres()));
    }

    // 노래 검색
    @Operation(summary = "노래 검색", description = "제목이나 아티스트로 노래를 검색하는 API")
    @GetMapping("/musics")
    public ResponseEntity<BaseResponse<SearchPageResponse<MusicSearchItemResponse>>> searchMusics(
            @RequestParam
            @NotBlank(message = "검색어는 필수 입력값입니다.")
            @Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
            String q,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
            @Max(value = 1000, message = "페이지는 1000 이하여야 합니다.")
            int page,
            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
            int size) {
        return ResponseEntity.ok(BaseResponse.success(200, "노래 검색 성공", musicService.searchMusics(q, page, size)));
    }

    // 아티스트 검색
    @Operation(summary = "아티스트 검색", description = "이름이나 별칭으로 아티스트를 검색하는 API")
    @GetMapping("/artists")
    public ResponseEntity<BaseResponse<SearchPageResponse<ArtistSearchItemResponse>>> searchArtists(
            @RequestParam
            @NotBlank(message = "검색어는 필수 입력값입니다.")
            @Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
            String q,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
            @Max(value = 1000, message = "페이지는 1000 이하여야 합니다.")
            int page,
            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
            int size) {
        return ResponseEntity.ok(BaseResponse.success(200, "아티스트 검색 성공", musicService.searchArtists(q, page, size)));
    }
}
