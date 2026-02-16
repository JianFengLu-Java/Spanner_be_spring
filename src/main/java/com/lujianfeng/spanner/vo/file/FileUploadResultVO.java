package com.lujianfeng.spanner.vo.file;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileUploadResultVO {
    private String objectName;
    private String url;
    private String contentType;
    private Long size;
}
