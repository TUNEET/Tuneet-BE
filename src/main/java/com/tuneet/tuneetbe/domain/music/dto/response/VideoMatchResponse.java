package com.tuneet.tuneetbe.domain.music.dto.response;

import java.util.List;

// 음악 API 매칭 응답 (요청과 같은 순서)
public record VideoMatchResponse(List<Result> results) {

    public static final String MATCHED = "matched";

    // status: matched, ambiguous, unmatched. music은 matched일 때만 있음
    public record Result(
            String videoId,
            String status,
            boolean isCover,
            String reason,
            MusicSearchItemResponse music) {

        public boolean isMatched() {
            return MATCHED.equals(status) && music != null;
        }
    }
}
