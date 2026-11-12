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

    @Schema(description = "작성자 프로필 사진 URL", example = "https://.../profile/abc.png", nullable = true)
    private String profilePicture;

    @Schema(description = "포스트한 노래")
    private MusicResponse music;

    @Schema(description = "코멘트 (최대 100자)", example = "요즘 계속 듣는 노래")
    private String content;

    @Schema(description = "작성 일시 ('n분 전' 표시는 프론트에서 계산)", example = "2026-11-12T14:20:00")
    private LocalDateTime createdAt;

    @Schema(description = "내가 작성한 포스트인지 여부 (삭제 버튼 표시용)", example = "true")
    private Boolean isMine;

    public static RoomPostResponse from(RoomPost roomPost, Long userId) {
        return RoomPostResponse.builder()
                .roomPostId(roomPost.getId())
                .nickname(roomPost.getUser().getNickname())
                .profilePicture(roomPost.getUser().getProfilePicture())
                .music(MusicResponse.from(roomPost.getMusic()))
                .content(roomPost.getRoomPostContent())
                .createdAt(roomPost.getCreatedAt())
                .isMine(roomPost.getUser().getId().equals(userId))
                .build();
    }
}
