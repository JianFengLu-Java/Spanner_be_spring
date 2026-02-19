package com.lujianfeng.spanner.dto.group;

import lombok.Data;

import java.util.List;

@Data
public class GroupBatchInviteRequestDTO {
    private List<String> accounts;
    private String source;
}
