package com.lujianfeng.spanner.dto.message;

import lombok.Data;

/**
 * Message Model
 * Simple demo
 *
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/15
 * @since 1.0
 */

@Data
public class MessageDTO {
    private String from;
    private String content;
}
