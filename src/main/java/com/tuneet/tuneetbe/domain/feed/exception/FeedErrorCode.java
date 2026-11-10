package com.tuneet.tuneetbe.domain.feed.exception;

import com.tuneet.tuneetbe.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeedErrorCode implements BaseErrorCode {

    FEED_NOT_FOUND("F4001", "피드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FEED_ACCESS_DENIED("F4002", "피드 접근 권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
