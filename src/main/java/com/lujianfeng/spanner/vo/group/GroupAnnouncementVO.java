package com.lujianfeng.spanner.vo.group;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GroupAnnouncementVO {
    private String announcementId;
    private String groupNo;
    private String content;
    private String publisherAccount;
    private String publisherName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
