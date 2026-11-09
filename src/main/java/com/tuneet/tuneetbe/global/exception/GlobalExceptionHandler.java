package com.tuneet.tuneetbe.global.exception;

import com.tuneet.tuneetbe.global.common.BaseResponse;
import com.tuneet.tuneetbe.global.exception.model.BaseErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 예외
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<Object>> handleCustomException(CustomException ex) {
        BaseErrorCode errorCode = ex.getErrorCode();
        log.warn("CustomException 발생: {} - {}", errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(BaseResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    // Validation 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> handleValidationException(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> fieldErrors.putIfAbsent(e.getField(), e.getDefaultMessage()));

        String message = fieldErrors.values().stream()
                .findFirst()
                .orElse(GlobalErrorCode.INVALID_INPUT_VALUE.getMessage());

        log.warn("Validation 오류 발생: {}", fieldErrors);
        return ResponseEntity.badRequest()
                .body(new BaseResponse<>(false, GlobalErrorCode.INVALID_INPUT_VALUE.getCode(), message, fieldErrors));
    }

    // 필수 요청 값이 누락되었을 때
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse<?>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        String message = String.format("필수 요청 값 '%s'이(가) 누락되었습니다.", ex.getParameterName());
        log.warn("요청 파라미터 누락: {}", ex.getParameterName());
        return ResponseEntity.badRequest()
                .body(new BaseResponse<>(false, GlobalErrorCode.INVALID_INPUT_VALUE.getCode(), message, Map.of(ex.getParameterName(), message)));
    }

    // 요청 파라미터 검증 실패
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<BaseResponse<?>> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex) {
        String message = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse(GlobalErrorCode.INVALID_INPUT_VALUE.getMessage());

        log.warn("요청 파라미터 검증 실패: {}", message);
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE.getCode(), message));
    }

    // 요청 값의 타입이 올바르지 않을 때
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<?>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        String message = String.format("요청 값 '%s'이(가) 올바르지 않습니다.", ex.getName());
        log.warn("요청 값 타입 오류: {}", ex.getName());
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE.getCode(), message));
    }

    // 업로드 파일이 누락되었을 때
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<BaseResponse<?>> handleMissingServletRequestPartException(
            MissingServletRequestPartException ex) {
        String message = String.format("필수 요청 값 '%s'이(가) 누락되었습니다.", ex.getRequestPartName());
        log.warn("요청 파일 누락: {}", ex.getRequestPartName());
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE.getCode(), message));
    }

    // 업로드 파일 크기를 초과했을 때
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<BaseResponse<?>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {
        log.warn("업로드 파일 크기 초과");
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE.getCode(), "이미지 크기는 5MB 이하여야 합니다."));
    }

    // 예상치 못한 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleException(Exception ex) {
        log.error("Server 오류 발생: ", ex);
        return ResponseEntity.status(GlobalErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(BaseResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), GlobalErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    // 잘못된 JSON 형식으로 요청 시
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error("JSON 파싱 에러 발생: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(BaseResponse.error("JSON400", "올바르지 않은 JSON 입력입니다."));
    }

    // 토큰이 누락되었을 때 발생하는 에러 처리
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<BaseResponse<Void>> handleMissingRequestHeaderException(Exception e) {
        return ResponseEntity
                .badRequest()
                .body(BaseResponse.error("TOKEN400", "헤더 정보가 누락되었습니다."));
    }
}