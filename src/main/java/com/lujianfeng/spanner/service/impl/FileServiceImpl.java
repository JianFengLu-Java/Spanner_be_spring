package com.lujianfeng.spanner.service.impl;

import com.lujianfeng.spanner.prop.MinIOProperties;
import com.lujianfeng.spanner.service.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;


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
        String fileName = file.getOriginalFilename();
        String suffixName = fileName != null && fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : "";
        String objectName =  UUID.randomUUID().toString().replace("-", "") +  suffixName;
        String contentType = file.getContentType();
        try{
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(inputStream,file.getSize(),-1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );

            return minioProperties.getEndpoint()+"/"+minioProperties.getBucketName()+"/"+objectName;




        }catch (Exception e) {
            e.printStackTrace();
        }




        return "";
    }
}
