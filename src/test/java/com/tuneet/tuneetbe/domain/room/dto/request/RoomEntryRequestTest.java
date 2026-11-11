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

class RoomEntryRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> messages(RoomEntryRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"1234", "0000"})
    @DisplayName("비밀번호가 없거나 숫자 4자리면 검증을 통과한다")
    void validPassword(String password) {
        assertThat(messages(new RoomEntryRequest("aB3kP9xQ", password))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "12345", "12a4"})
    @DisplayName("비밀번호가 숫자 4자리가 아니면 검증에 실패한다")
    void invalidPassword(String password) {
        assertThat(messages(new RoomEntryRequest("aB3kP9xQ", password)))
                .containsExactly("비밀번호는 숫자 4자리여야 합니다.");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("초대 코드가 비어 있으면 검증에 실패한다")
    void blankInviteCode(String inviteCode) {
        assertThat(messages(new RoomEntryRequest(inviteCode, null)))
                .containsExactly("초대 코드는 필수 입력값입니다.");
    }

    @Test
    @DisplayName("초대 코드가 20자를 넘으면 검증에 실패한다")
    void tooLongInviteCode() {
        assertThat(messages(new RoomEntryRequest("a".repeat(21), null)))
                .containsExactly("올바른 초대 코드가 아닙니다.");
    }
}
