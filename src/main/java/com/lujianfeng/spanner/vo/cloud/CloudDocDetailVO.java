package com.lujianfeng.spanner.vo.cloud;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudDocDetailVO {
    private String id;
    private String title;
    private String contentHtml;
    private String contentJson;
    private String createdAt;
    private String updatedAt;
    private String lastSavedAt;
    private Long version;
    private String ownerAccount;
    private Boolean editable;
}
