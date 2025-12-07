package com.lujianfeng.spanner.prop;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "minio")
@Setter @Getter
public class MinIOProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
}
