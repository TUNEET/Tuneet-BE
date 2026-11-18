package com.tuneet.tuneetbe.domain.music.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VideoMatchItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("길이·연도가 없으면 요청 본문에서 빠지고 topicChannel은 보내지 않는다")
    void serialize() throws Exception {
        VideoMatchItem item = new VideoMatchItem("v1", "Heroine", "back number - Topic");

        assertThat(objectMapper.writeValueAsString(item))
                .isEqualTo("{\"videoId\":\"v1\",\"title\":\"Heroine\",\"channel\":\"back number - Topic\"}");
        assertThat(objectMapper.writeValueAsString(item.withDetails(270, 2015)))
                .isEqualTo("{\"videoId\":\"v1\",\"title\":\"Heroine\",\"channel\":\"back number - Topic\","
                        + "\"durationSeconds\":270,\"releaseYear\":2015}");
    }
}
