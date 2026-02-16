package com.lujianfeng.spanner.vo.moment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MomentUserVO {
    private String account;
    private String name;
    private String avatar;
}
