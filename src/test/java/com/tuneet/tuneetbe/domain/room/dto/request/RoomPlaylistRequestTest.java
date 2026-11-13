package com.tuneet.tuneetbe.domain.room.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RoomPlaylistRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> messages(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("노래 id가 UUID 형식이면 검증을 통과한다")
    void validMusicId() {
        assertThat(messages(new RoomPlaylistCreateRequest("27e95178-49ac-47a1-8b4c-540c77982680"))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("노래 id가 비어 있으면 검증에 실패한다")
    void blankMusicId(String musicId) {
        assertThat(messages(new RoomPlaylistCreateRequest(musicId)))
                .contains("노래는 필수 입력값입니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "27E95178-49AC-47A1-8B4C-540C77982680", "27e95178-49ac-47a1-8b4c"})
    @DisplayName("노래 id가 UUID 형식이 아니면 검증에 실패한다")
    void invalidMusicId(String musicId) {
        assertThat(messages(new RoomPlaylistCreateRequest(musicId)))
                .containsExactly("올바른 노래 id가 아닙니다.");
    }

    @Test
    @DisplayName("순서 변경 id 목록이 있으면 검증을 통과한다")
    void validOrder() {
        assertThat(messages(new RoomPlaylistPosUpdateRequest(List.of(3L, 1L, 2L)))).isEmpty();
    }

    @Test
    @DisplayName("순서 변경 id 목록이 없으면 검증에 실패한다")
    void nullOrder() {
        assertThat(messages(new RoomPlaylistPosUpdateRequest(null)))
                .containsExactly("플레이리스트 순서는 필수 입력값입니다.");
    }
}
