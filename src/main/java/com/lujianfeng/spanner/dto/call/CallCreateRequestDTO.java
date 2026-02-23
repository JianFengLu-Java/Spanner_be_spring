package com.lujianfeng.spanner.dto.call;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallCreateRequestDTO {
    private String requestId;
    private String calleeAccount;
    private String type;
}
