package com.example.infinite.global.s3.s3error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum S3ErrorCode implements ErrorCodeType { // 프로젝트의 인터페이스를 따르세요!
    FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S3_001", "파일 업로드 중 오류가 발생했습니다."),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "S3_002", "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "S3_003", "파일 용량이 제한을 초과했습니다.");

    private final HttpStatus Status;
    private final String code;
    private final String message;

}
