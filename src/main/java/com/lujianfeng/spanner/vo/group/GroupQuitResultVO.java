package com.lujianfeng.spanner.vo.group;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupQuitResultVO {
    private String groupNo;
    private Boolean quit;
    private Boolean shouldRemoveLocalSession;
}
