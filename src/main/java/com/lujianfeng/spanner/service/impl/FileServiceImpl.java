package com.lujianfeng.spanner.service.impl;

import com.lujianfeng.spanner.prop.MinIOProperties;
import com.lujianfeng.spanner.service.service.FileService;
import com.lujianfeng.spanner.vo.file.FileObjectVO;
import com.lujianfeng.spanner.vo.file.FileUploadResultVO;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.StatObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;


/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/16
 * @since 1.0
 */

@Service
public class FileServiceImpl implements FileService {

    private final MinIOProperties minioProperties;
    private final MinioClient minioClient;

    public FileServiceImpl(@Autowired MinioClient minioClient,
                           @Autowired MinIOProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }


    @Override
    public String upload(MultipartFile file) {
        try {
            return uploadImage(file).getUrl();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public FileUploadResultVO uploadImage(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String suffixName = fileName != null && fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")).toLowerCase(Locale.ROOT) : "";
        String objectName = UUID.randomUUID().toString().replace("-", "") + suffixName;
        String contentType = file.getContentType();
        String normalizedType = contentType != null ? contentType : "application/octet-stream";
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(normalizedType)
                            .build()
            );

            String publicUrl = minioProperties.getDomainUrl() + "/" + minioProperties.getBucketName() + "/" + objectName;
            return FileUploadResultVO.builder()
                    .objectName(objectName)
                    .url(publicUrl)
                    .contentType(normalizedType)
                    .size(file.getSize())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public FileObjectVO getImage(String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
            try (GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            )) {
                byte[] data = response.readAllBytes();
                String contentType = stat.contentType() != null ? stat.contentType() : "application/octet-stream";
                return FileObjectVO.builder()
                        .objectName(objectName)
                        .contentType(contentType)
                        .size(stat.size())
                        .data(data)
                        .build();
            }
        } catch (Exception e) {
            throw new RuntimeException("文件读取失败", e);
        }
    }
}
