package com.example.infinite.domain.artistcontent.media.service;

import com.example.infinite.domain.artistcontent.media.config.MediaStorageProperties;
import com.example.infinite.domain.artistcontent.media.entity.Media;
import com.example.infinite.domain.artistcontent.media.enums.MediaType;
import com.example.infinite.domain.artistcontent.media.repository.MediaRepository;
import com.example.infinite.domain.artistcontent.media.storage.ObjectStorageClient;
import com.example.infinite.domain.artistcontent.media.storage.UploadedObject;
import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.artistcontent.post.fanletter.entity.FanLetter;
import com.example.infinite.domain.artistcontent.post.fanpost.entity.FanPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.IntConsumer;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
/*
 * 미디어 흐름의 중심 서비스다.
 *
 * 책임:
 * 1) 파일 정책 검증
 * 2) object storage 업로드/삭제 위임
 * 3) Media 메타데이터 row 저장/삭제
 * 4) 게시글의 mediaCount 비정규화 컬럼 반영
 *
 * 호출 위치:
 * - FanPostService.create/update/delete
 * - 이후 ArtistPostService.create/update/delete 도 같은 패턴으로 재사용 가능
 *
 * 이 서비스는 "파일 인프라"와 "게시글 정책"의 경계층이다.
 * 그래서 S3 SDK 세부사항은 모르고, 반대로 컨트롤러는 파일 정책 세부사항을 몰라도 된다.
 */
public class MediaService {

    private final MediaRepository mediaRepository;
    private final ObjectStorageClient objectStorageClient;
    private final MediaStorageProperties mediaStorageProperties;

    @Transactional
    public void attachFanPostMedia(Long artistId, FanPost fanPost, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        // Media 는 targetType/targetId 메타데이터를 저장하므로
        // fanPost id 가 생성된 "게시글 저장 직후"에만 첨부를 연결할 수 있다.
        uploadPostMedia(artistId, PostType.FAN_POST, fanPost.getId(), files, fanPost::changeMediaCountBy);
    }

    @Transactional
    public void replaceFanPostMedia(Long artistId, FanPost fanPost, List<MultipartFile> files) {
        // 전체 교체 정책은 유지하되,
        // 새 첨부 저장 성공 전에는 기존 DB 참조를 지우지 않아야 업로드 실패 시 깨지지 않는다.
        replacePostMedia(artistId, PostType.FAN_POST, fanPost.getId(), files, fanPost::changeMediaCountBy);
    }

    @Transactional
    public void deleteFanPostMedia(FanPost fanPost) {
        // 게시글 삭제 시 DB 메타데이터와 storage 파일을 함께 정리해 dangling media 를 남기지 않는다.
        deletePostMedia(PostType.FAN_POST, fanPost.getId(), fanPost::changeMediaCountBy);
    }

    @Transactional
    public void attachArtistPostMedia(Long artistId, ArtistPost artistPost, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        // ArtistPost 역시 Media 구조를 그대로 재사용한다.
        // "누가 쓸 수 있는가" 검증은 상위 서비스가 하고, 여기서는 업로드만 담당한다.
        uploadPostMedia(artistId, PostType.ARTIST_POST, artistPost.getId(), files, artistPost::changeMediaCountBy);
    }

    @Transactional
    public void replaceArtistPostMedia(Long artistId, ArtistPost artistPost, List<MultipartFile> files) {
        // ArtistPost 도 같은 전체 교체 정책을 따르되,
        // 교체 실패 시 기존 첨부 참조가 살아 있도록 새 첨부를 먼저 확보한다.
        replacePostMedia(artistId, PostType.ARTIST_POST, artistPost.getId(), files, artistPost::changeMediaCountBy);
    }

    @Transactional
    public void deleteArtistPostMedia(ArtistPost artistPost) {
        deletePostMedia(PostType.ARTIST_POST, artistPost.getId(), artistPost::changeMediaCountBy);
    }

    @Transactional
    public void attachFanLetterMedia(Long artistId, FanLetter fanLetter, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_FAN_LETTER_IMAGE_ONLY);
        }
        // 팬레터는 사진 카드 1장을 본문처럼 쓰는 모델이라
        // fan post 처럼 List<MultipartFile> 을 받지 않고 단일 파일만 받는다.
        uploadFanLetterMedia(artistId, fanLetter.getId(), file);
    }

    @Transactional
    public void replaceFanLetterMedia(Long artistId, FanLetter fanLetter, MultipartFile file) {
        // 팬레터도 전체 교체지만, 새 이미지 확보 전에 기존 media row 를 지우면 실패 시 참조가 깨진다.
        replaceFanLetterImage(artistId, fanLetter, file);
    }

    @Transactional
    public void deleteFanLetterMedia(FanLetter fanLetter) {
        deletePostMedia(PostType.FAN_LETTER, fanLetter.getId(), ignored -> {
        });
    }

    private void uploadPostMedia(
            Long artistId,
            PostType targetType,
            Long targetId,
            List<MultipartFile> files,
            IntConsumer mediaCountChanger
    ) {
        // targetType 만 바꾸면 FanPost / ArtistPost 모두 처리할 수 있게 공통 업로드 엔진으로 만든다.
        // 상위 서비스는 "어느 게시글인가"만 넘기고,
        // 파일 정책 검증/업로드/DB 저장은 여기서 일괄 처리한다.
        // sortOrder 는 기존 개수 뒤에 이어 붙여 충돌 없이 순서를 유지한다.
        List<Media> existingMedia = mediaRepository.findByTargetTypeAndTargetIdOrderBySortOrderAsc(targetType, targetId);
        List<PreparedUploadFile> preparedFiles = prepareUploadFiles(existingMedia, files);
        int startSortOrder = existingMedia.size();

        List<UploadedObject> uploadedObjects = new ArrayList<>();
        try {
            // 업로드 일부 성공 후 DB 저장에서 실패하면 storage 에 orphan file 이 남을 수 있다.
            // 그래서 업로드된 객체 목록을 모아 두고, 실패 시 보상 삭제(cleanup)를 시도한다.
            List<Media> savedMedia = new ArrayList<>();
            for (int index = 0; index < preparedFiles.size(); index++) {
                PreparedUploadFile preparedFile = preparedFiles.get(index);
                String storageKey = buildStorageKey(artistId, targetType, targetId, preparedFile.extension());
                UploadedObject uploadedObject = objectStorageClient.upload(preparedFile.file(), storageKey);
                uploadedObjects.add(uploadedObject);

                // DB 에는 조회에 필요한 메타데이터만 남긴다.
                // 실제 파일 바이너리와 저장 위치 관리는 object storage 가 책임진다.
                savedMedia.add(Media.create(
                        targetType,
                        targetId,
                        preparedFile.mediaType(),
                        uploadedObject.key(),
                        uploadedObject.url(),
                        null,
                        resolveOriginalFileName(preparedFile.file()),
                        uploadedObject.contentType(),
                        uploadedObject.size(),
                        startSortOrder + index
                ));
            }

            mediaRepository.saveAll(savedMedia);
            // post 엔티티의 mediaCount 는 비정규화 컬럼이므로
            // 실제 저장된 media row 수만큼 같은 트랜잭션 안에서 함께 반영한다.
            mediaCountChanger.accept(savedMedia.size());
        } catch (RuntimeException e) {
            // DB 저장 실패나 storage 예외가 나면 이미 올라간 파일부터 best-effort 로 정리한다.
            cleanupUploadedObjects(uploadedObjects);
            throw e;
        }
    }

    private void deletePostMedia(PostType targetType, Long targetId, IntConsumer mediaCountChanger) {
        List<Media> existingMedia = mediaRepository.findByTargetTypeAndTargetIdOrderBySortOrderAsc(targetType, targetId);
        if (existingMedia.isEmpty()) {
            return;
        }

        // 현재 수정 정책이 "전체 삭제 후 재업로드"이므로
        // target 단위 일괄 삭제를 기본 연산으로 둔다.
        mediaRepository.deleteAllInBatch(existingMedia);
        mediaCountChanger.accept(-existingMedia.size());
        deleteObjectsQuietly(existingMedia);
    }

    private void replacePostMedia(
            Long artistId,
            PostType targetType,
            Long targetId,
            List<MultipartFile> files,
            IntConsumer mediaCountChanger
    ) {
        List<Media> existingMedia = mediaRepository.findByTargetTypeAndTargetIdOrderBySortOrderAsc(targetType, targetId);
        if (files == null || files.isEmpty()) {
            deletePostMedia(targetType, targetId, mediaCountChanger);
            return;
        }

        // 전체 교체에서는 기존 첨부를 모두 버리고 새 집합으로 다시 만들기 때문에
        // 정책 검증도 "새 파일 집합만 놓고 최종 상태가 유효한가"를 본다.
        List<PreparedUploadFile> preparedFiles = prepareUploadFiles(List.of(), files);
        List<UploadedObject> uploadedObjects = new ArrayList<>();

        try {
            List<Media> newMedia = new ArrayList<>();
            for (int index = 0; index < preparedFiles.size(); index++) {
                PreparedUploadFile preparedFile = preparedFiles.get(index);
                String storageKey = buildStorageKey(artistId, targetType, targetId, preparedFile.extension());
                UploadedObject uploadedObject = objectStorageClient.upload(preparedFile.file(), storageKey);
                uploadedObjects.add(uploadedObject);

                newMedia.add(Media.create(
                        targetType,
                        targetId,
                        preparedFile.mediaType(),
                        uploadedObject.key(),
                        uploadedObject.url(),
                        null,
                        resolveOriginalFileName(preparedFile.file()),
                        uploadedObject.contentType(),
                        uploadedObject.size(),
                        index
                ));
            }

            mediaRepository.saveAll(newMedia);
            mediaRepository.deleteAllInBatch(existingMedia);
            mediaCountChanger.accept(newMedia.size() - existingMedia.size());
            deleteObjectsQuietly(existingMedia);
        } catch (RuntimeException e) {
            cleanupUploadedObjects(uploadedObjects);
            throw e;
        }
    }

    private void uploadFanLetterMedia(Long artistId, Long fanLetterId, MultipartFile file) {
        List<Media> existingMedia = mediaRepository.findByTargetTypeAndTargetIdOrderBySortOrderAsc(PostType.FAN_LETTER, fanLetterId);
        if (!existingMedia.isEmpty()) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_FAN_LETTER_IMAGE_ONLY);
        }

        // 정책상 팬레터는 비디오를 허용하지 않는다.
        // prepareUploadFile 로 기본 검증을 통과한 뒤에도 IMAGE 타입인지 한 번 더 좁혀서 확인한다.
        PreparedUploadFile preparedFile = prepareUploadFile(file);
        if (preparedFile.mediaType() != MediaType.IMAGE) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_FAN_LETTER_IMAGE_ONLY);
        }

        UploadedObject uploadedObject = objectStorageClient.upload(
                preparedFile.file(),
                buildStorageKey(artistId, PostType.FAN_LETTER, fanLetterId, preparedFile.extension())
        );

        try {
            mediaRepository.save(Media.create(
                    PostType.FAN_LETTER,
                    fanLetterId,
                    MediaType.IMAGE,
                    uploadedObject.key(),
                    uploadedObject.url(),
                    null,
                    resolveOriginalFileName(preparedFile.file()),
                    uploadedObject.contentType(),
                    uploadedObject.size(),
                    0
            ));
        } catch (RuntimeException e) {
            cleanupUploadedObjects(List.of(uploadedObject));
            throw e;
        }
    }

    private void replaceFanLetterImage(Long artistId, FanLetter fanLetter, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_FAN_LETTER_IMAGE_ONLY);
        }

        List<Media> existingMedia = mediaRepository.findByTargetTypeAndTargetIdOrderBySortOrderAsc(
                PostType.FAN_LETTER,
                fanLetter.getId()
        );

        PreparedUploadFile preparedFile = prepareUploadFile(file);
        if (preparedFile.mediaType() != MediaType.IMAGE) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_FAN_LETTER_IMAGE_ONLY);
        }

        UploadedObject uploadedObject = objectStorageClient.upload(
                preparedFile.file(),
                buildStorageKey(artistId, PostType.FAN_LETTER, fanLetter.getId(), preparedFile.extension())
        );

        try {
            mediaRepository.save(Media.create(
                    PostType.FAN_LETTER,
                    fanLetter.getId(),
                    MediaType.IMAGE,
                    uploadedObject.key(),
                    uploadedObject.url(),
                    null,
                    resolveOriginalFileName(preparedFile.file()),
                    uploadedObject.contentType(),
                    uploadedObject.size(),
                    0
            ));
            mediaRepository.deleteAllInBatch(existingMedia);
            deleteObjectsQuietly(existingMedia);
        } catch (RuntimeException e) {
            cleanupUploadedObjects(List.of(uploadedObject));
            throw e;
        }
    }

    private List<PreparedUploadFile> prepareUploadFiles(List<Media> existingMedia, List<MultipartFile> files) {
        // 각 파일을 먼저 IMAGE/VIDEO 로 분류한 뒤,
        // 그 분류 결과를 기준으로 혼합 업로드 금지/최대 개수 제한을 검사한다.
        List<PreparedUploadFile> preparedFiles = files.stream()
                .map(this::prepareUploadFile)
                .toList();

        validateCombinedPolicy(existingMedia, preparedFiles);
        return preparedFiles;
    }

    private PreparedUploadFile prepareUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_INVALID_FORMAT);
        }
        // 확장자와 MIME 타입을 함께 본다.
        // 둘 중 하나만 믿으면 우회가 쉬워서
        // "image/* + 허용 이미지 확장자"처럼 교차 검증한다.
        String originalFileName = resolveOriginalFileName(file);
        String extension = extractExtension(originalFileName);
        String contentType = file.getContentType();

        if (contentType != null && contentType.startsWith("image/")
                && mediaStorageProperties.allowedImageExtensions().contains(extension)) {
            if (file.getSize() > mediaStorageProperties.maxImageSizeBytes()) {
                throw new ArtistContentException(ArtistContentErrorCode.MEDIA_SIZE_EXCEEDED);
            }
            // 이후 단계에서는 다시 MIME/확장자를 계산하지 않도록
            // "검증 완료된 파일 정보"를 PreparedUploadFile 로 묶어 넘긴다.
            return new PreparedUploadFile(file, MediaType.IMAGE, extension);
        }

        if (contentType != null && contentType.startsWith("video/")
                && mediaStorageProperties.allowedVideoExtensions().contains(extension)) {
            if (file.getSize() > mediaStorageProperties.maxVideoSizeBytes()) {
                throw new ArtistContentException(ArtistContentErrorCode.MEDIA_SIZE_EXCEEDED);
            }
            return new PreparedUploadFile(file, MediaType.VIDEO, extension);
        }

        throw new ArtistContentException(ArtistContentErrorCode.MEDIA_INVALID_FORMAT);
    }

    private void validateCombinedPolicy(List<Media> existingMedia, List<PreparedUploadFile> preparedFiles) {
        // 같은 게시글 안에서는 "이미지 여러 장" 또는 "비디오 1개"만 허용한다.
        // 이 정책이 있어야 프론트 렌더링 규칙과 수정 규칙이 단순해진다.
        boolean hasImage = existingMedia.stream().anyMatch(media -> media.getMediaType() == MediaType.IMAGE)
                || preparedFiles.stream().anyMatch(file -> file.mediaType() == MediaType.IMAGE);
        boolean hasVideo = existingMedia.stream().anyMatch(media -> media.getMediaType() == MediaType.VIDEO)
                || preparedFiles.stream().anyMatch(file -> file.mediaType() == MediaType.VIDEO);

        if (hasImage && hasVideo) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_MIXED_TYPE_NOT_ALLOWED);
        }

        int totalCount = existingMedia.size() + preparedFiles.size();
        if (hasImage && totalCount > mediaStorageProperties.maxImageCount()) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_MAX_IMAGE_COUNT_EXCEEDED);
        }
        if (hasVideo && totalCount > mediaStorageProperties.maxVideoCount()) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_MAX_VIDEO_COUNT_EXCEEDED);
        }
    }

    private void cleanupUploadedObjects(Collection<UploadedObject> uploadedObjects) {
        // DB 저장 전에 일부 파일만 올라간 상태에서 예외가 나면 orphan file 이 생기므로
        // best-effort 로 즉시 정리한다.
        for (UploadedObject uploadedObject : uploadedObjects) {
            try {
                objectStorageClient.delete(uploadedObject.key());
            } catch (RuntimeException e) {
                log.warn("업로드 실패 보상 삭제 중 일부 파일 정리 실패: key={}", uploadedObject.key());
            }
        }
    }

    private void deleteObjectsQuietly(List<Media> existingMedia) {
        for (Media media : existingMedia) {
            try {
                objectStorageClient.delete(media.getStorageKey());
            } catch (RuntimeException e) {
                // DB 삭제 이후의 storage 정리는 best-effort 로 두고,
                // orphan file 은 운영에서 추적 가능하게 로그만 남긴다.
                log.warn("미디어 삭제 후 스토리지 정리 실패: key={}", media.getStorageKey());
            }
        }
    }

    private String buildStorageKey(Long artistId, PostType targetType, Long targetId, String extension) {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        // 원본 파일명은 신뢰하지 않고,
        // artist/target/date/uuid 조합으로 key 를 만들어 충돌과 경로 오염을 막는다.
        return "%s/artist-%d/%s/%d/%s.%s".formatted(
                mediaStorageProperties.keyPrefix(),
                artistId,
                targetType.name().toLowerCase(Locale.ROOT),
                targetId,
                today + "-" + UUID.randomUUID(),
                extension
        );
    }

    private String extractExtension(String fileName) {
        int extensionStartIndex = fileName.lastIndexOf('.');
        if (extensionStartIndex < 0 || extensionStartIndex == fileName.length() - 1) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_INVALID_FORMAT);
        }
        // 이후 정책 비교는 모두 소문자 확장자 기준으로 통일한다.
        return fileName.substring(extensionStartIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String resolveOriginalFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_INVALID_FORMAT);
        }
        // 공백만 있는 파일명은 허용하지 않고, 앞뒤 공백만 제거한 값만 사용한다.
        return originalFileName.strip();
    }

    private record PreparedUploadFile(
            MultipartFile file,
            MediaType mediaType,
            String extension
    ) {
        // 업로드 전 검증을 끝낸 "안전한 파일 묶음"이다.
        // 이후 단계에서는 이 record 만 보고도 mediaType/확장자를 다시 계산할 필요가 없다.
    }
}
