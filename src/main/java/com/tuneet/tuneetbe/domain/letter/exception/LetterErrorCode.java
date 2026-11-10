package com.tuneet.tuneetbe.domain.letter.exception;

import com.tuneet.tuneetbe.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LetterErrorCode implements BaseErrorCode {

    LETTER_NOT_FOUND("L4001", "편지가 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    LETTER_SELF("L4002", "나에게는 편지를 보낼 수 없습니다.", HttpStatus.BAD_REQUEST),
    LETTER_NOT_RECEIVABLE("L4003", "편지를 받을 수 없는 사용자입니다.", HttpStatus.BAD_REQUEST),
    LETTER_DAILY_LIMIT_EXCEEDED("L4004", "오늘 보낼 수 있는 편지를 모두 사용했습니다.", HttpStatus.BAD_REQUEST),
    LETTER_NO_RANDOM_RECEIVER("L4005", "지금은 편지를 받을 수 있는 사용자가 없습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
