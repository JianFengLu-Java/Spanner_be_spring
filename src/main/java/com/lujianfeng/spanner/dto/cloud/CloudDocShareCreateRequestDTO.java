package com.lujianfeng.spanner.dto.cloud;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CloudDocShareCreateRequestDTO {
    private String friendAccount;
    private Integer expireHours;
    private String shareMode;
}
