package com.tuneet.tuneetbe.domain.room.dto.response;

import com.tuneet.tuneetbe.domain.room.entity.RoomPlaylist;
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
@Schema(title = "방 플레이리스트 목록 응답 dto", description = "방 플레이리스트를 재생 순서대로 조회할 때 서버에서 보내는 결과")
public class RoomPlaylistListResponse {

    @Schema(description = "플레이리스트 곡 목록 (position 순)")
    private List<RoomPlaylistResponse> playlists;

    public static RoomPlaylistListResponse from(List<RoomPlaylist> playlists) {
        return RoomPlaylistListResponse.builder()
                .playlists(playlists.stream().map(RoomPlaylistResponse::from).toList())
                .build();
    }
}
