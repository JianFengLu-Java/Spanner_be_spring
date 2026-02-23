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
public class MessageRecallResultVO {
    private String messageId;
    private String messageType;
    private String groupNo;
    private String from;
    private String to;
    private Boolean recalled;
    private LocalDateTime recalledAt;
    private LocalDateTime recallDeadlineAt;
}
