package com.example.infinite.domain.artistcontent.media.storage;

import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import org.springframework.web.multipart.MultipartFile;

/*
 * object storage 가 꺼져 있을 때 사용하는 fallback 구현체다.
 *
 * 목적은 "앱 부팅은 허용하되, 실제 업로드/삭제는 명확히 실패시키는 것"이다.
 * 즉 개발자가 S3 설정 없이 로컬 실행은 할 수 있지만,
 * 미디어 기능을 사용하려 하면 MEDIA_STORAGE_NOT_CONFIGURED 예외를 받는다.
 */
public class DisabledObjectStorageClient implements ObjectStorageClient {

    @Override
    public UploadedObject upload(MultipartFile file, String key) {
        // storage 미설정 상태에서 업로드를 시도하면 조용히 무시하지 않고
        // 원인을 바로 알 수 있는 도메인 예외를 던진다.
        throw new ArtistContentException(ArtistContentErrorCode.MEDIA_STORAGE_NOT_CONFIGURED);
    }

    @Override
    public void delete(String key) {
        // 삭제 역시 동일하게 "지원하지 않음"이 아니라 "설정되지 않음" 의미를 명확히 전달한다.
        throw new ArtistContentException(ArtistContentErrorCode.MEDIA_STORAGE_NOT_CONFIGURED);
    }
}
