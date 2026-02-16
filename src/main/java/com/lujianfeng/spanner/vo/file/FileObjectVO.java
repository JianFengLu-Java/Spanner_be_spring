package com.lujianfeng.spanner.vo.file;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileObjectVO {
    private String objectName;
    private String contentType;
    private Long size;
    private byte[] data;
}
