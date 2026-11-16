package com.tuneet.tuneetbe.domain.music.dto.request;

// 음악 API 매칭 요청 항목 (유튜브 영상 하나)
public record VideoMatchItem(
        String videoId,
        String title,
        String channel) { // " - Topic"을 포함한 채널 이름 그대로
}
