package com.lujianfeng.spanner.dto.task;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
public class TaskEventRequestDTO {
    private String eventId;
    private String eventType;
    private String bizId;
    private Long actorUserId;
    private String targetId;
    private Instant createdAt;
    private Map<String, Object> meta;
}
