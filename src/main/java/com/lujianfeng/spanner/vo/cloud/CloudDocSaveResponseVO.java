package com.lujianfeng.spanner.vo.cloud;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudDocSaveResponseVO {
    private String id;
    private String updatedAt;
    private String lastSavedAt;
    private Long version;
}
