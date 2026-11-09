package com.tuneet.tuneetbe.domain.music.exception;

import com.tuneet.tuneetbe.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MusicErrorCode implements BaseErrorCode {

    MUSIC_API_UNAVAILABLE("M5001", "음악 서버와 통신할 수 없습니다.", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
