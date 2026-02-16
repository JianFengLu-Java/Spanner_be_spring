package com.lujianfeng.spanner.service.service;

import com.lujianfeng.spanner.vo.file.FileObjectVO;
import com.lujianfeng.spanner.vo.file.FileUploadResultVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface FileService {
    String upload(MultipartFile file);

    FileUploadResultVO uploadImage(MultipartFile file);

    FileObjectVO getImage(String objectName);

}
