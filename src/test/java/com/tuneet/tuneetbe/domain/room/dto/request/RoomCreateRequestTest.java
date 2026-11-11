package com.tuneet.tuneetbe.domain.room.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RoomCreateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> messages(RoomCreateRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    private RoomCreateRequest withPassword(String password) {
        return new RoomCreateRequest("퇴근길 플리 모임", "퇴근길에 듣는 노래를 공유해요", null, password);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"1234", "0000"})
    @DisplayName("비밀번호가 없거나 숫자 4자리면 검증을 통과한다")
    void validPassword(String password) {
        assertThat(messages(withPassword(password))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "12345", "12a4", "１２３４"})
    @DisplayName("비밀번호가 숫자 4자리가 아니면 검증에 실패한다")
    void invalidPassword(String password) {
        assertThat(messages(withPassword(password))).containsExactly("비밀번호는 숫자 4자리여야 합니다.");
    }

    @Test
    @DisplayName("방 이름이 비어 있으면 검증에 실패한다")
    void blankTitle() {
        assertThat(messages(new RoomCreateRequest(" ", null, null, null)))
                .containsExactly("방 이름은 필수 입력값입니다.");
    }

    @Test
    @DisplayName("방 이름이 50자를 넘으면 검증에 실패한다")
    void tooLongTitle() {
        assertThat(messages(new RoomCreateRequest("가".repeat(51), null, null, null)))
                .containsExactly("방 이름은 50자 이하여야 합니다.");
    }

    @Test
    @DisplayName("방 소개가 200자를 넘으면 검증에 실패한다")
    void tooLongDescription() {
        assertThat(messages(new RoomCreateRequest("퇴근길 플리 모임", "가".repeat(201), null, null)))
                .containsExactly("방 소개는 200자 이하여야 합니다.");
    }
}
