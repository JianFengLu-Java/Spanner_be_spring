package com.lujianfeng.spanner.vo.cloud;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudDocWsCursorVO {
    private Integer anchor;
    private Integer head;
}
