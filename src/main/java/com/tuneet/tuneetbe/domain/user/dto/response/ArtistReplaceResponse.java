package com.tuneet.tuneetbe.domain.user.dto.response;

import com.tuneet.tuneetbe.domain.music.dto.response.ArtistResponse;
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
@Schema(title = "선호 아티스트 교체 응답 dto", description = "선호 아티스트를 교체했을때 서버에서 보내는 결과")
public class ArtistReplaceResponse {

    @Schema(description = "교체된 선호 아티스트 목록")
    private List<ArtistResponse> artists;
}
