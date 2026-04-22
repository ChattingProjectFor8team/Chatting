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
@RequestMapping("/files")
public class FileController {

    private final S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(@RequestPart("file") MultipartFile file) {
        String fileUrl = s3Service.upload(file); // S3에 업로드 후 URL 반환

        // 미리 만들어두신 레코드(FileUploadResponse)를 활용해 응답!
        return ResponseEntity.ok(ApiResponse.success(new FileUploadResponse(fileUrl)));
    }

    @GetMapping("/download-url")
    public ResponseEntity<ApiResponse<FileDownloadUrlResponse>> getDownloadUrl(@RequestParam("fileName") String fileName) {
        URL downloadUrl = s3Service.getDownloadUrl(fileName);
        return ResponseEntity.ok(ApiResponse.success(new FileDownloadUrlResponse(downloadUrl.toString())));
    }
}
