package com.tuneet.tuneetbe.domain.music.dto.response;

import com.tuneet.tuneetbe.domain.music.entity.Music;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "음악 응답 dto", description = "음악 정보")
public class MusicResponse {

    @Schema(description = "노래 검색 결과의 id", example = "27e95178-49ac-47a1-8b4c-540c77982680")
    private String musicId;

    @Schema(description = "노래 제목", example = "노래 제목")
    private String title;

    @Schema(description = "아티스트 이름", example = "아티스트")
    private String artist;

    @Schema(description = "앨범 커버 이미지 url", example = "https://...")
    private String musicPicture;

    @Schema(description = "재생용 유튜브 영상 ID. 없으면 null", example = "jeqdYqsrsA0", nullable = true)
    private String videoId;

    @Schema(description = "내가 좋아요한 곡인지 (같은 곡의 다른 음원, 유튜브에서 불러온 좋아요 포함). 목록·상세 조회에서만 채우고 그 외 응답은 null",
            example = "true", nullable = true)
    private Boolean liked;

    public void markLiked(boolean liked) {
        this.liked = liked;
    }

    public static MusicResponse from(Music music) {
        return MusicResponse.builder()
                .musicId(music.getExternalId())
                .title(music.getTitle())
                .artist(music.getArtist())
                .musicPicture(music.getMusicPicture())
                .videoId(music.getYoutubeVideoId())
                .build();
    }
}
