package com.tuneet.tuneetbe.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "프로필 추가 설정 응답 dto", description = "프로필 추가 설정 데이터를 보냈을때 서버에서 보내는 결과")
public class ProfileAddResponse {

    @Schema(description = "사용자 고유번호", example = "1")
    private Long userId;

    @Schema(description = "닉네임", example = "test01")
    private String nickname;

    @Schema(description = "프로필 사진 url", example = "https://...")
    private String profilePicture;

    @Schema(description = "프로필 추가 설정 시간", example = "2026-11-07T10:40:00")
    private LocalDateTime modifiedAt;
}
