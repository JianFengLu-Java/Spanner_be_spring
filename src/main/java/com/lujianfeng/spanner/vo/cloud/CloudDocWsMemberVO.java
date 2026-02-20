package com.lujianfeng.spanner.vo.cloud;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudDocWsMemberVO {
    private String account;
    private CloudDocWsCursorVO cursor;
}
