package com.tuneet.tuneetbe.domain.feed.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum SentMessageType {

    @Schema(description = "피드에 보낸 코멘트")
    COMMENT,

    @Schema(description = "코멘트나 답장에 보낸 답장")
    REPLY
}
