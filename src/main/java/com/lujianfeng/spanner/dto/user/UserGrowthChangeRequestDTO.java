package com.lujianfeng.spanner.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserGrowthChangeRequestDTO {

    private Long growthValue;

    private String reason;
}
