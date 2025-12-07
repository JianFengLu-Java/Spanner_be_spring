package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.service.service.FileService;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传
 */

@Slf4j
@RestController
@RequestMapping("/files")
public class FileController {


    private final FileService fileService;

    public FileController(@Autowired FileService fileService) {
        this.fileService = fileService;
    }

    /**
     *  前端上传文件
     * @param file 参数名为fileData
     * @return JSON对象
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String,Object>> updata(@RequestParam("fileData") MultipartFile file) {
        try{
            String fileUrl =  fileService.upload(file);
            log.info(fileUrl);
            return ResponseEntity.ok(
                    Map.of(
                            "status","success",
                            "fileUrl",fileUrl
                    )
            );
        }catch(Exception e){
            log.error(e.getMessage());
        }
        return ResponseEntity.ok(
                    Map.of(
                            "message","error"
                    )
        );
    }
}
