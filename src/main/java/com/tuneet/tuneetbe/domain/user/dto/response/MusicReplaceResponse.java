package com.tuneet.tuneetbe.domain.user.dto.response;

import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "좋아하는 노래 교체 응답 dto", description = "좋아하는 노래를 교체했을때 서버에서 보내는 결과")
public class MusicReplaceResponse {

    @Schema(description = "교체된 좋아하는 노래 목록")
    private List<MusicResponse> musics;
}
