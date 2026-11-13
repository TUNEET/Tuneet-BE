package com.tuneet.tuneetbe.domain.room.dto.response;

import com.tuneet.tuneetbe.domain.room.entity.RoomPost;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "방 포스트 목록 응답 dto", description = "방 포스트 목록을 최신순으로 조회할 때 서버에서 보내는 결과")
public class RoomPostListResponse {

    @Schema(description = "포스트 목록")
    private List<RoomPostResponse> roomPosts;

    @Schema(description = "다음 포스트 존재 여부", example = "true")
    private Boolean hasNext;

    public static RoomPostListResponse from(Slice<RoomPost> posts, Long userId, Set<Long> roomUserIds) {
        return RoomPostListResponse.builder()
                .roomPosts(posts.getContent().stream()
                        .map(roomPost -> RoomPostResponse.from(roomPost, userId, roomUserIds))
                        .toList())
                .hasNext(posts.hasNext())
                .build();
    }
}
