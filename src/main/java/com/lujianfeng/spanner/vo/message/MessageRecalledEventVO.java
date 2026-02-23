package com.lujianfeng.spanner.vo.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRecalledEventVO {
    private String eventType;
    private String messageType;
    private String messageId;
    private String groupNo;
    private String from;
    private String to;
    private Boolean recalled;
    private LocalDateTime recalledAt;
}
