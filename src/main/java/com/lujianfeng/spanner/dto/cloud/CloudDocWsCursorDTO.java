package com.lujianfeng.spanner.dto.cloud;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CloudDocWsCursorDTO {
    private String docId;
    private Integer anchor;
    private Integer head;
}
