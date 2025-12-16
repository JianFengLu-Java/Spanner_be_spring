package com.lujianfeng.spanner.config;

import com.lujianfeng.spanner.prop.MinIOProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO Container configuration class
 *
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/15
 * @since 1.0
 */

@Configuration
public class MinIOConfiguration {

    private final MinIOProperties minioProperties;

    public MinIOConfiguration(@Autowired MinIOProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient = null;
        String bucketName = minioProperties.getBucketName();

        try {
            //Create MinioClient instance
            //Chained calls
            minioClient = new MinioClient.Builder()
                    //Set MinIO server endpoint
                    .endpoint(minioProperties.getEndpoint())
                    //Set AccessKey and SecretKey
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                    //Create Minio Client Object
                    .build();

            //Check if the bucket exists
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            //If the bucket don't exist,create it.
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                System.out.println("MinIO Bucket created successfully: " + bucketName);
            }

            // Return MinIO Client Object.
            return minioClient;

        } catch (Exception e) {
            //Failure handling:throw runtime exception,stop application launching,and exposed error
            System.err.println("MinIO Client initialization failed for bucket: " + bucketName);
            System.err.println("Error details: " + e.getMessage());
            //Throw runtime exception,make sure Spring container can't finish loading.
            throw new RuntimeException("MinIO Client configuration failed.", e);
        }
    }
}