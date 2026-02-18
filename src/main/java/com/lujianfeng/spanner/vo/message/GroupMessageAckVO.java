package com.lujianfeng.spanner.vo.message;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GroupMessageAckVO {
    private String clientMessageId;
    private String messageId;
    private String groupNo;
    private String status;
    private LocalDateTime ackAt;
}
