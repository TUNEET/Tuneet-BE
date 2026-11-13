package com.tuneet.tuneetbe.domain.room.exception;

import com.tuneet.tuneetbe.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RoomErrorCode implements BaseErrorCode {

    ROOM_NOT_FOUND("R4001", "존재하지 않는 방입니다.", HttpStatus.NOT_FOUND),
    ROOM_PASSWORD_REQUIRED("R4002", "비밀번호가 필요한 방입니다.", HttpStatus.BAD_REQUEST),
    ROOM_PASSWORD_MISMATCH("R4003", "비밀번호가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    NOT_ROOM_MEMBER("R4004", "방 멤버가 아닙니다.", HttpStatus.FORBIDDEN),
    ROOM_POST_NOT_FOUND("R4005", "존재하지 않는 포스트입니다.", HttpStatus.NOT_FOUND),
    ROOM_POST_ACCESS_DENIED("R4006", "포스트 삭제 권한이 없습니다.", HttpStatus.FORBIDDEN),
    ROOM_PLAYLIST_DUPLICATE_MUSIC("R4007", "이미 플레이리스트에 있는 곡입니다.", HttpStatus.CONFLICT),
    ROOM_PLAYLIST_NOT_FOUND("R4008", "플레이리스트에 없는 곡입니다.", HttpStatus.NOT_FOUND),
    ROOM_PLAYLIST_ACCESS_DENIED("R4009", "곡을 추가한 사람이나 방장만 삭제할 수 있습니다.", HttpStatus.FORBIDDEN),
    ROOM_OWNER_ONLY("R4010", "방장만 할 수 있는 작업입니다.", HttpStatus.FORBIDDEN),
    ROOM_PLAYLIST_ORDER_MISMATCH("R4011", "플레이리스트가 변경되었습니다. 새로고침 후 다시 시도해 주세요.", HttpStatus.CONFLICT),

    INVITE_CODE_GENERATION_FAILED("R5001", "초대 코드 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
