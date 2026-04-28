package com.example.infinite.domain.artistcontent.media.storage;

/*
 * object storage 업로드 결과를 담는 값 객체다.
 *
 * MediaService 는 업로드 후 이 값을 받아 Media 엔티티를 만든다.
 * 즉 "실제 파일 저장 결과"와 "DB 메타데이터 저장" 사이를 이어 주는 전달용 객체다.
 */
public record UploadedObject(
        String key,
        String url,
        String contentType,
        long size
) {
}
