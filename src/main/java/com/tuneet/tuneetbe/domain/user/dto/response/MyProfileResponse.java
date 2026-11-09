package com.tuneet.tuneetbe.domain.user.dto.response;

import com.tuneet.tuneetbe.domain.music.dto.response.ArtistResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.GenreResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "내 정보 조회 응답 dto", description = "사용자가 본인의 프로필을 조회할때 서버에서 보내는 결과")
public class MyProfileResponse {

    @Schema(description = "사용자 고유번호", example = "1")
    private Long userId;

    @Schema(description = "닉네임", example = "test01")
    private String nickname;

    @Schema(description = "프로필 사진 url", example = "https://...")
    private String profilePicture;

    @Schema(description = "프로필 음악")
    private MusicResponse profileMusic;

    @Schema(description = "선호 장르 목록")
    private List<GenreResponse> genres;

    @Schema(description = "좋아하는 아티스트 목록")
    private List<ArtistResponse> favoriteArtists;
}
