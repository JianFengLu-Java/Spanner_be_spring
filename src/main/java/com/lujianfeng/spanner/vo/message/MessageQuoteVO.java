package com.lujianfeng.spanner.vo.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageQuoteVO {
    private String messageId;
    private String from;
    private String content;
}
