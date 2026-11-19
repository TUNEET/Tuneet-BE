package com.tuneet.tuneetbe.domain.feed.exception;

import com.tuneet.tuneetbe.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeedErrorCode implements BaseErrorCode {

    FEED_NOT_FOUND("F4001", "피드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FEED_ACCESS_DENIED("F4002", "피드 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    FEED_MESSAGE_SELF("F4003", "본인 피드에는 메시지를 보낼 수 없습니다.", HttpStatus.BAD_REQUEST),
    FEED_MESSAGE_NOT_FOUND("F4004", "코멘트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FEED_MESSAGE_REPLY_NOT_FOUND("F4005", "답장을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FEED_MESSAGE_ALREADY_REPLIED("F4006", "이미 답장한 메시지입니다.", HttpStatus.CONFLICT),
    FEED_MESSAGE_REPLY_TO_DELETED_USER("F4007", "탈퇴한 사용자에게는 답장할 수 없습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
