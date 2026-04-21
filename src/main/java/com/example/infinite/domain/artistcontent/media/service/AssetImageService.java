package com.example.infinite.domain.artistcontent.media.service;

import com.example.infinite.domain.artistcontent.media.config.MediaStorageProperties;
import com.example.infinite.domain.artistcontent.media.storage.ObjectStorageClient;
import com.example.infinite.domain.artistcontent.media.storage.UploadedObject;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
/*
 * Member / Artist / ArtistMember 의 프로필/커버 이미지를 위한 업로드 서비스다.
 *
 * 이 서비스는 Media 엔티티를 만들지 않는다.
 * 즉 "게시글 첨부파일"이 아니라 "단일 대표 이미지"를 object storage 에 올리고 URL만 돌려준다.
 *
 * 게시글 첨부와의 차이:
 * - 게시글 첨부: Media row 를 만들고 targetType/targetId 로 연결한다.
 * - 대표 이미지: 각 엔티티의 profileImageUrl / coverImageUrl 필드에 URL 문자열만 저장한다.
 *
 * 그래서 이 서비스의 책임은 단순하다.
 * 1) "이미지 파일만" 허용하는 검증
 * 2) 도메인별 폴더 경로를 반영한 storage key 생성
 * 3) object storage 업로드 위임
 * 4) 기존 URL을 storage key 로 되돌릴 수 있으면 삭제 지원
 */
public class AssetImageService {

    private final ObjectStorageClient objectStorageClient;
    private final MediaStorageProperties mediaStorageProperties;

    @Transactional
    public String uploadMemberProfileImage(Long memberId, MultipartFile file) {
        // 일반 멤버 프로필 이미지는 members/{memberId}/profile 경로 아래에 저장한다.
        return uploadImage("members/%d/profile".formatted(memberId), file);
    }

    @Transactional
    public String uploadMemberCoverImage(Long memberId, MultipartFile file) {
        // 일반 멤버 커버 이미지는 members/{memberId}/cover 경로 아래에 저장한다.
        return uploadImage("members/%d/cover".formatted(memberId), file);
    }

    @Transactional
    public String uploadArtistProfileImage(Long artistId, MultipartFile file) {
        // 아티스트 대표 프로필 이미지는 artist 단위 폴더로 구분한다.
        return uploadImage("artists/%d/profile".formatted(artistId), file);
    }

    @Transactional
    public String uploadArtistCoverImage(Long artistId, MultipartFile file) {
        // 아티스트 커버도 profile 과 분리된 prefix 를 써서 운영에서 구분이 쉽게 한다.
        return uploadImage("artists/%d/cover".formatted(artistId), file);
    }

    @Transactional
    public String uploadArtistMemberProfileImage(Long artistId, Long memberId, MultipartFile file) {
        // 아티스트 멤버 프로필은 artist 하위 member 폴더에 넣어 같은 커뮤니티 안에서 추적하기 쉽게 둔다.
        return uploadImage("artists/%d/members/%d/profile".formatted(artistId, memberId), file);
    }

    public void deleteByUrlQuietly(String fileUrl) {
        // 대표 이미지 교체 시 "기존 파일 정리"가 필요하지만,
        // URL 이 외부 CDN 주소이거나 key 역추적이 불가능할 수도 있으므로 조용한(best-effort) 삭제로 둔다.
        String storageKey = extractStorageKey(fileUrl);
        if (!StringUtils.hasText(storageKey)) {
            return;
        }

        try {
            objectStorageClient.delete(storageKey);
        } catch (RuntimeException e) {
            log.warn("대표 이미지 정리 실패: url={}, key={}", fileUrl, storageKey);
        }
    }

    private String uploadImage(String folderPath, MultipartFile file) {
        // 대표 이미지도 내부적으로는 "검증 -> key 생성 -> storage 업로드 -> URL 반환" 순서를 동일하게 따른다.
        PreparedImageUpload preparedImageUpload = prepareImage(file);
        String storageKey = buildStorageKey(folderPath, preparedImageUpload.extension());
        UploadedObject uploadedObject = objectStorageClient.upload(preparedImageUpload.file(), storageKey);
        return uploadedObject.url();
    }

    private PreparedImageUpload prepareImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_INVALID_FORMAT);
        }

        // 대표 이미지는 비디오를 허용하지 않고, 게시글 첨부와 달리 "이미지 1개"만 대상이다.
        // 그래도 확장자와 MIME type 을 함께 봐서 단순 파일명 우회를 막는다.
        String originalFileName = resolveOriginalFileName(file);
        String extension = extractExtension(originalFileName);
        String contentType = file.getContentType();

        if (contentType == null
                || !contentType.startsWith("image/")
                || !mediaStorageProperties.allowedImageExtensions().contains(extension)) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_INVALID_FORMAT);
        }

        if (file.getSize() > mediaStorageProperties.maxImageSizeBytes()) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_SIZE_EXCEEDED);
        }

        return new PreparedImageUpload(file, extension);
    }

    private String buildStorageKey(String folderPath, String extension) {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        // 원본 파일명은 쓰지 않고 date + uuid 로 key 를 만든다.
        // 대표 이미지는 나중에 여러 번 교체될 수 있으므로 충돌 회피가 중요하다.
        return "%s/%s/%s.%s".formatted(
                mediaStorageProperties.keyPrefix(),
                folderPath,
                today + "-" + UUID.randomUUID(),
                extension
        );
    }

    private String extractStorageKey(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }

        // publicBaseUrl 기반 URL 을 쓰는 환경이면 그 prefix 를 제거해 storage key 로 되돌린다.
        if (StringUtils.hasText(mediaStorageProperties.publicBaseUrl())) {
            String publicBaseUrl = mediaStorageProperties.publicBaseUrl().replaceAll("/+$", "");
            String prefix = publicBaseUrl + "/";
            if (fileUrl.startsWith(prefix)) {
                return fileUrl.substring(prefix.length());
            }
        }

        // custom publicBaseUrl 이 없으면 기본 S3 public URL 규칙에서 key 를 잘라낸다.
        String defaultS3Prefix = "https://%s.s3.%s.amazonaws.com/".formatted(
                mediaStorageProperties.bucket(),
                mediaStorageProperties.region()
        );
        if (fileUrl.startsWith(defaultS3Prefix)) {
            return fileUrl.substring(defaultS3Prefix.length());
        }

        return null;
    }

    private String extractExtension(String fileName) {
        int extensionStartIndex = fileName.lastIndexOf('.');
        if (extensionStartIndex < 0 || extensionStartIndex == fileName.length() - 1) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_INVALID_FORMAT);
        }
        // 대표 이미지도 정책 비교는 항상 소문자 확장자 기준으로 맞춘다.
        return fileName.substring(extensionStartIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String resolveOriginalFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFileName)) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_INVALID_FORMAT);
        }
        // multipart 업로드의 원본 파일명은 비어 있을 수 있어 방어가 필요하다.
        return originalFileName.strip();
    }

    private record PreparedImageUpload(
            MultipartFile file,
            String extension
    ) {
        // 한 번 검증이 끝난 대표 이미지 파일 묶음이다.
        // 이후 단계에서는 extension 을 다시 계산하지 않고 그대로 사용한다.
    }
}
