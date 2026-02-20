package com.lujianfeng.spanner.vo.cloud;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudDocSummaryVO {
    private String id;
    private String title;
    private String snippet;
    private String createdAt;
    private String updatedAt;
    private String lastSavedAt;
    private Long version;
    private Boolean deleted;
}
