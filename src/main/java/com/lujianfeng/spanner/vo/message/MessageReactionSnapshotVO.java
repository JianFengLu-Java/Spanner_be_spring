package com.lujianfeng.spanner.vo.message;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MessageReactionSnapshotVO {
    private Long chatId;
    private String messageId;
    private String serverMessageId;
    private String clientMessageId;
    private List<ReactionItemVO> reactions;
}
