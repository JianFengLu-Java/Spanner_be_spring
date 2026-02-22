package com.lujianfeng.spanner.vo.message;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class MessageReactionUpdatedEventVO {
    private String eventType;
    private Long chatId;
    private String messageId;
    private String serverMessageId;
    private Instant updatedAt;
    private List<ReactionItemVO> reactions;
}
