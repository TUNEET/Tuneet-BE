package com.tuneet.tuneetbe.domain.room.dto.response;

import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.room.entity.RoomPost;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "방 포스트 dto", description = "방 홈 화면과 포스트 목록에서 포스트 하나를 표현하는 DTO")
public class RoomPostResponse {

    @Schema(description = "방 포스트 고유번호", example = "1")
    private Long roomPostId;

    @Schema(description = "작성자 닉네임", example = "튜닛")
    private String nickname;

    @Schema(description = "포스트한 노래")
    private MusicResponse music;

    @Schema(description = "코멘트 (최대 100자)", example = "요즘 계속 듣는 노래")
    private String content;

    @Schema(description = "작성 일시 ('n분 전' 표시는 프론트에서 계산)", example = "2026-11-12T14:20:00")
    private LocalDateTime createdAt;

    public static RoomPostResponse from(RoomPost roomPost) {
        return RoomPostResponse.builder()
                .roomPostId(roomPost.getId())
                .nickname(roomPost.getUser().getNickname())
                .music(MusicResponse.from(roomPost.getMusic()))
                .content(roomPost.getRoomPostContent())
                .createdAt(roomPost.getCreatedAt())
                .build();
    }
}
