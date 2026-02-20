package com.lujianfeng.spanner.dto.cloud;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CloudDocSaveRequestDTO {
    private String title;
    private String contentHtml;
    private String contentJson;
    private Long baseVersion;
}
