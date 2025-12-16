package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.message.MessageDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * Chat Controller
 *
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/15
 * @since 1.0
 */

@Controller
public class ChatController {
    private static final Logger log = LogManager.getLogger(ChatController.class);

    @MessageMapping("chat.send")
    @SendTo("/topic/public")
    public MessageDTO sendMessage(MessageDTO message) {

        log.info("Received the MessageDTO{}", message);
        log.info("Received the Message Content:{}", message.getContent());

        return message;
    }
}
