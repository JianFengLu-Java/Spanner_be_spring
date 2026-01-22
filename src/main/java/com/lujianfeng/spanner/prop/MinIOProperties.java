package com.lujianfeng.spanner.prop;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 *
 * MinIO Server Properties
 *
 * @author lujianfeng
 * @version 1.0
 * @date 2025-12-15 23:28
 * @description get prop from application.yaml
 *
 */

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "minio")

public class MinIOProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String domainUrl;

}
