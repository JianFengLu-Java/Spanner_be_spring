package com.lujianfeng.spanner.vo.group;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GroupBatchInviteResultVO {
    private List<String> successAccounts;
    private List<GroupBatchInviteFailedVO> failed;
}
