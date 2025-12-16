package com.lujianfeng.spanner.dto.message;

/**
 * Message Model
 * Simple demo
 *
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/15
 * @since 1.0
 */

public class MessageDTO {
    private String from;
    private String content;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
