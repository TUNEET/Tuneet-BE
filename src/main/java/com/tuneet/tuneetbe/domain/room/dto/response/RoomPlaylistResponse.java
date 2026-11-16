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

    @Schema(description = "내가 추가한 곡인지", example = "true")
    private Boolean isMine;

    @Schema(description = "내가 삭제할 수 있는 곡인지 (방장이거나 내가 추가한 곡). 삭제 버튼은 이 값으로 표시", example = "true")
    private Boolean deletable;

    public static RoomPlaylistResponse from(RoomPlaylist roomPlaylist, Long userId, boolean isOwner) {
        boolean isMine = roomPlaylist.getUser().getId().equals(userId);

        return RoomPlaylistResponse.builder()
                .roomPlaylistId(roomPlaylist.getId())
                .position(roomPlaylist.getPosition())
                .music(MusicResponse.from(roomPlaylist.getMusic()))
                .isMine(isMine)
                .deletable(isOwner || isMine)
                .build();
    }
}
