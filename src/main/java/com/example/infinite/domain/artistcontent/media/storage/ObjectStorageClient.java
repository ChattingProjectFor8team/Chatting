package com.example.infinite.domain.artistcontent.media.storage;

import org.springframework.web.multipart.MultipartFile;

/*
 * MediaService 가 바라보는 스토리지 표준 인터페이스다.
 *
 * 중요한 점은 서비스 계층이 S3 SDK 타입을 직접 모르도록 끊어 준다는 것이다.
 * 지금 구현체는 S3ObjectStorageClient 이지만,
 * 나중에 Cloudflare R2/MinIO/테스트용 Fake 구현으로 바뀌어도
 * MediaService 는 이 인터페이스만 유지하면 된다.
 */
public interface ObjectStorageClient {

    // 파일 바이너리를 실제 저장소에 업로드하고,
    // DB media row 저장에 필요한 최소 메타데이터(key/url/contentType/size)를 되돌려준다.
    UploadedObject upload(MultipartFile file, String key);

    // DB media row 를 지울 때 object storage 의 실제 파일도 함께 정리할 수 있게
    // storage key 기준 삭제 연산을 제공한다.
    void delete(String key);
}
