package com.lujianfeng.spanner.dto.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageRecallRequestDTO {
    /**
     * 消息类型：PRIVATE / GROUP
     */
    private String messageType;

    /**
     * 群消息撤回时可选透传；若传入需与消息归属群一致
     */
    private String groupNo;
}
