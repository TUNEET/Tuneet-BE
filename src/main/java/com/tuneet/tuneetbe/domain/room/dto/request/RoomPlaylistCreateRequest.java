package com.tuneet.tuneetbe.domain.room.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(title = "방 플레이리스트 생성 요청 dto", description = "사용자가 방 플레이리스트 생성시 보내는 데이터")
public class RoomPlaylistCreateRequest {

    @Schema(description = "노래 검색 결과의 id", example = "27e95178-49ac-47a1-8b4c-540c77982680")
    @NotBlank(message = "노래는 필수 입력값입니다.")
    @Pattern(regexp = "^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$", message = "올바른 노래 id가 아닙니다.")
    private String musicId;
}
