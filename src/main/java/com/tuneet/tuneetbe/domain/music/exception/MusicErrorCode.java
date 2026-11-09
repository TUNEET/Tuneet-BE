package com.tuneet.tuneetbe.domain.music.exception;

import com.tuneet.tuneetbe.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MusicErrorCode implements BaseErrorCode {

    MUSIC_NOT_FOUND("M4001", "존재하지 않는 노래입니다.", HttpStatus.BAD_REQUEST),
    ARTIST_NOT_FOUND("M4002", "존재하지 않는 아티스트입니다.", HttpStatus.BAD_REQUEST),
    MUSIC_API_UNAVAILABLE("M5001", "음악 서버와 통신할 수 없습니다.", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
