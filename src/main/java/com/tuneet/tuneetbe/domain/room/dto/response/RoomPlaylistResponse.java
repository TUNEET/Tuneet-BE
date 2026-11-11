package com.tuneet.tuneetbe.domain.room.dto.response;

import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.room.entity.RoomPlaylist;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "방 플레이리스트 dto", description = "방 홈 화면과 플레이리스트 목록에서 곡 하나를 표현하는 DTO")
public class RoomPlaylistResponse {

    @Schema(description = "방 플레이리스트 항목 고유번호", example = "1")
    private Long roomPlaylistId;

    @Schema(description = "재생 순서 (작을수록 앞)", example = "1")
    private Integer position;

    @Schema(description = "노래 정보")
    private MusicResponse music;

    public static RoomPlaylistResponse from(RoomPlaylist roomPlaylist) {
        return RoomPlaylistResponse.builder()
                .roomPlaylistId(roomPlaylist.getId())
                .position(roomPlaylist.getPosition())
                .music(MusicResponse.from(roomPlaylist.getMusic()))
                .build();
    }
}
