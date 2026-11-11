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

    INVITE_CODE_GENERATION_FAILED("R5001", "초대 코드 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
