package com.tuneet.tuneetbe.domain.user.exception;

import com.tuneet.tuneetbe.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    USER_DUPLICATE("U4001", "회원가입이 되어 있는 사용자입니다.", HttpStatus.CONFLICT),
    USER_NOT_FOUND("U4002", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
    EMAIL_DUPLICATE("U4003", "가입된 이메일입니다.", HttpStatus.CONFLICT),
    PASSWORD_MISMATCH("U4004", "올바른 비밀번호를 입력해 주세요.", HttpStatus.BAD_REQUEST),
    NICKNAME_DUPLICATE("U4005", "중복된 닉네임입니다.", HttpStatus.CONFLICT),
    GENRE_NOT_FOUND("U4006", "존재하지 않는 장르입니다.", HttpStatus.NOT_FOUND),
    LOCATION_AGREEMENT_REQUIRED("U4007", "위치정보 수집 및 이용 동의가 필요합니다.", HttpStatus.BAD_REQUEST),
    SELF_PROFILE_NOT_ALLOWED("U4008", "본인의 프로필은 내 정보 조회를 이용해 주세요.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
