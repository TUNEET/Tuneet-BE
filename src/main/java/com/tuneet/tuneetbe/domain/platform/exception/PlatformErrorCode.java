package com.tuneet.tuneetbe.domain.platform.exception;

import com.tuneet.tuneetbe.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlatformErrorCode implements BaseErrorCode {

    PLATFORM_INVALID_STATE("P4001", "연동 요청이 만료되었거나 올바르지 않습니다. 다시 연결해 주세요.", HttpStatus.BAD_REQUEST),
    PLATFORM_SCOPE_DENIED("P4002", "유튜브 좋아요 목록 읽기 권한을 허용해야 연동할 수 있습니다.", HttpStatus.BAD_REQUEST),
    PLATFORM_AUTH_FAILED("P4003", "유튜브 계정 인증에 실패했습니다. 다시 연결해 주세요.", HttpStatus.BAD_REQUEST),
    PLATFORM_AUTH_EXPIRED("P4004", "유튜브 연동이 해제되었습니다. 다시 연결해 주세요.", HttpStatus.UNAUTHORIZED),
    PLATFORM_NOT_CONNECTED("P4005", "연동된 음원 플랫폼이 없습니다.", HttpStatus.NOT_FOUND),
    PLATFORM_API_UNAVAILABLE("P5001", "유튜브와 통신할 수 없습니다.", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
