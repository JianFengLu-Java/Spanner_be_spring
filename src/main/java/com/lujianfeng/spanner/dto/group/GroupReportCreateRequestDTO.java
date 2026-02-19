package com.lujianfeng.spanner.dto.group;

import lombok.Data;

import java.util.List;

@Data
public class GroupReportCreateRequestDTO {
    private String reasonType;
    private String description;
    private List<String> evidenceUrls;
}
