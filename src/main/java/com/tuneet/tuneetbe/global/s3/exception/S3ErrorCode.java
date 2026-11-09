package com.tuneet.tuneetbe.global.s3.exception;

import com.tuneet.tuneetbe.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum S3ErrorCode implements BaseErrorCode {

    S3_UPLOAD_FAILED("S4001", "이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    EMPTY_IMAGE("S4002", "빈 파일은 업로드할 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_TYPE("S4003", "JPG, PNG, WEBP 이미지만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
