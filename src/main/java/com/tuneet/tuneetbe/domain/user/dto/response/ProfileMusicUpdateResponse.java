package com.tuneet.tuneetbe.domain.user.dto.response;

import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "프로필 음악 설정 응답 dto", description = "프로필 음악을 설정했을때 서버에서 보내는 결과")
public class ProfileMusicUpdateResponse {

    @Schema(description = "설정된 프로필 음악")
    private MusicResponse profileMusic;
}
