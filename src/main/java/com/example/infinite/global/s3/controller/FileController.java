package com.example.infinite.global.s3.controller;

import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.s3.dto.response.FileDownloadUrlResponse;
import com.example.infinite.global.s3.dto.response.FileUploadResponse;
import com.example.infinite.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;

@RestController
@RequiredArgsConstructor
public class FileController {

    private final S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = s3Service.upload(file);

        return ResponseEntity.ok(ApiResponse.success(fileUrl));
    }

    @GetMapping("/download-url/{fileName}")
    public ResponseEntity<ApiResponse<String>> getDownloadUrl(@PathVariable String fileName) {
        String downloadUrl = String.valueOf(s3Service.getDownloadUrl(fileName));

        return ResponseEntity.ok(ApiResponse.success(downloadUrl));
    }
}
