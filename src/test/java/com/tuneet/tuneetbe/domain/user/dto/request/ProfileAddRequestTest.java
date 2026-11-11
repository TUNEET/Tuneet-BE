package com.tuneet.tuneetbe.domain.user.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileAddRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> messages(ProfileAddRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("닉네임이 있으면 검증을 통과한다")
    void validNickname() {
        assertThat(messages(new ProfileAddRequest("튜니", null))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("닉네임이 없거나 공백이면 검증에 실패한다")
    void blankNickname(String nickname) {
        assertThat(messages(new ProfileAddRequest(nickname, "new.png")))
                .containsExactly("닉네임은 필수 입력값입니다.");
    }

    @Test
    @DisplayName("닉네임이 50자를 넘으면 검증에 실패한다")
    void tooLongNickname() {
        assertThat(messages(new ProfileAddRequest("가".repeat(51), null)))
                .containsExactly("닉네임은 50자 이하여야 합니다.");
    }
}
