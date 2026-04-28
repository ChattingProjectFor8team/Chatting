package com.example.infinite.domain.artistcontent.media.storage;

import com.example.infinite.domain.artistcontent.media.config.MediaStorageProperties;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

/*
 * ObjectStorageClient 의 S3 구현체다.
 *
 * 책임 범위는 의도적으로 좁다.
 * - 한다: S3 PUT/DELETE 호출, 업로드 결과 메타데이터 반환
 * - 하지 않는다: 파일 개수 제한, MIME/확장자 검증, 권한 검증
 *
 * 즉 "인프라 연동"만 책임지고, 비즈니스 정책은 MediaService 쪽에 남겨 둔다.
 */
@Slf4j
public class S3ObjectStorageClient implements ObjectStorageClient {

    private final S3Client s3Client;
    private final MediaStorageProperties properties;

    public S3ObjectStorageClient(S3Client s3Client, MediaStorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public UploadedObject upload(MultipartFile file, String key) {
        try {
            // 파일 바이트는 S3 에 저장하고, 이후 DB 에 남길 메타데이터만 반환한다.
            // thumbnail 생성, media 정책 검증 같은 일은 여기서 하지 않는다.
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            return new UploadedObject(
                    key,
                    buildPublicUrl(key),
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException | RuntimeException e) {
            log.error(
                    "S3 upload failed: bucket={}, key={}, region={}, contentType={}, size={}, causeType={}, message={}",
                    properties.bucket(),
                    key,
                    properties.region(),
                    file.getContentType(),
                    file.getSize(),
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String key) {
        try {
            // 서비스는 key 하나만 넘기면 되고, bucket 세부사항은 이 구현체 내부에 숨긴다.
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(key)
                    .build());
        } catch (RuntimeException e) {
            log.error(
                    "S3 delete failed: bucket={}, key={}, region={}, causeType={}, message={}",
                    properties.bucket(),
                    key,
                    properties.region(),
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_UPLOAD_FAILED);
        }
    }

    private String buildPublicUrl(String key) {
        // CDN 또는 커스텀 도메인이 있으면 그 URL을 우선 사용한다.
        // 없으면 기본 S3 public URL 규칙으로 조립한다.
        if (StringUtils.hasText(properties.publicBaseUrl())) {
            return properties.publicBaseUrl().replaceAll("/+$", "") + "/" + key;
        }
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                properties.bucket(),
                properties.region(),
                key
        );
    }
}
