package com.lujianfeng.spanner.dto.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageReactionToggleRequestDTO {
    private Long chatId;
    private String serverMessageId;
    private String clientMessageId;
    private ReactionDTO reaction;
    private String operatorId;
    private String requestId;
}
