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
public class GroupMessageVO {
    private String messageId;
    private String groupNo;
    private String from;
    private String formName;
    private String fromName;
    private String fromRealName;
    private String fromAvatarUrl;
    private String content;
    private MessageQuoteVO quote;
    private String clientMessageId;
    private LocalDateTime sentAt;
    private Boolean recalled;
    private LocalDateTime recalledAt;
}
