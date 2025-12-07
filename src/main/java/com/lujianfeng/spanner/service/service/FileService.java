package com.lujianfeng.spanner.service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface FileService {
    String upload(MultipartFile file);

}
