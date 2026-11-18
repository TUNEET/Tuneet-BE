package com.tuneet.tuneetbe.domain.music.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Locale;

// 음악 API 매칭 요청 항목 (유튜브 영상 하나)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VideoMatchItem(
        String videoId,
        String title,
        String channel, // " - Topic"을 포함한 채널 이름 그대로
        Integer durationSeconds, // 제목으로 못 찾은 Topic 영상을 다시 매칭할 때만
        Integer releaseYear) {

    public VideoMatchItem(String videoId, String title, String channel) {
        this(videoId, title, channel, null, null);
    }

    @JsonIgnore
    public boolean isTopicChannel() {
        return channel != null && channel.toLowerCase(Locale.ROOT).endsWith(" - topic");
    }

    public VideoMatchItem withDetails(int durationSeconds, Integer releaseYear) {
        return new VideoMatchItem(videoId, title, channel, durationSeconds, releaseYear);
    }
}
