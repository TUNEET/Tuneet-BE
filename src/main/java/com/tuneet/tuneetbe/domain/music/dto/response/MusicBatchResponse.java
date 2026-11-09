package com.tuneet.tuneetbe.domain.music.dto.response;

import java.util.List;

public record MusicBatchResponse(List<MusicSearchItemResponse> items, List<String> missingIds) {
}
