package com.tuneet.tuneetbe.domain.user.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TasteReplaceRequestTest {

    private static final String ID_1 = "27e95178-49ac-47a1-8b4c-540c77982680";
    private static final String ID_2 = "b9545342-1e6d-4dae-84ac-013374ad8d7c";
    private static final String ID_3 = "0d79fe8e-ba27-4859-bb8c-2f255f346853";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private LinkedHashSet<String> ids(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    private <T> Set<String> messages(Set<ConstraintViolation<T>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(java.util.stream.Collectors.toSet());
    }

    @Test
    @DisplayName("좋아하는 노래 목록에 null이 있으면 검증에 실패한다")
    void musicIdsRejectNull() {
        var violations = validator.validate(new MusicReplaceRequest(ids(ID_1, null)));

        assertThat(messages(violations)).containsExactly("올바른 노래 id가 아닙니다.");
    }

    @Test
    @DisplayName("선호 아티스트 목록에 null이 있으면 검증에 실패한다")
    void artistIdsRejectNull() {
        var violations = validator.validate(new ArtistReplaceRequest(ids(ID_1, ID_2, null)));

        assertThat(messages(violations)).containsExactly("올바른 아티스트 id가 아닙니다.");
    }

    @Test
    @DisplayName("프로필 음악 id가 없거나 형식이 다르면 검증에 실패한다")
    void profileMusicIdRejectsInvalid() {
        assertThat(messages(validator.validate(new ProfileMusicUpdateRequest(null))))
                .containsExactly("노래는 필수 입력값입니다.");
        assertThat(messages(validator.validate(new ProfileMusicUpdateRequest("12"))))
                .containsExactly("올바른 노래 id가 아닙니다.");
        assertThat(validator.validate(new ProfileMusicUpdateRequest(ID_1))).isEmpty();
    }

    @Test
    @DisplayName("올바른 ID 목록은 검증을 통과한다")
    void validIds() {
        assertThat(validator.validate(new MusicReplaceRequest(ids(ID_1)))).isEmpty();
        assertThat(validator.validate(new ArtistReplaceRequest(ids(ID_1, ID_2, ID_3)))).isEmpty();
    }
}
