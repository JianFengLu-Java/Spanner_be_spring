package com.lujianfeng.spanner.config;

import com.lujianfeng.spanner.prop.MinIOProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs; // 引入 MakeBucketArgs
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinIOConfiguration {

    private final MinIOProperties minioProperties;

    public MinIOConfiguration(@Autowired MinIOProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient = null; // 在 try 块外部声明
        String bucketName = minioProperties.getBucketName();

        try {
            // 1. 创建 MinioClient 实例
            minioClient = new MinioClient.Builder()
                    .endpoint(minioProperties.getEndpoint())
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                    .build();

            // 2. 检查 Bucket 是否存在
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            // 3. 如果 Bucket 不存在，则创建它
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                System.out.println("MinIO Bucket created successfully: " + bucketName);
            }

            return minioClient; // 成功返回 Client

        } catch (Exception e) {
            // 4. 失败处理：抛出运行时异常，阻止应用启动，并暴露配置错误
            System.err.println("MinIO Client initialization failed for bucket: " + bucketName);
            System.err.println("Error details: " + e.getMessage());
            // 抛出运行时异常，确保 Spring 容器无法完成加载
            throw new RuntimeException("MinIO Client configuration failed.", e);
        }
    }
}