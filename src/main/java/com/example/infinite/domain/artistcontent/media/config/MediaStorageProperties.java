package com.example.infinite.domain.artistcontent.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * media.storage.* 아래 설정을 한 객체로 모아 두는 "설명서" 역할의 설정 객체다.
 *
 * 이 클래스는 파일 업로드를 수행하지 않는다.
 * 대신 아래 두 종류의 값을 다른 클래스가 쓰기 좋게 정리해 둔다.
 * 1) S3 연결값: bucket, region, endpoint, credential
 * 2) 업로드 정책값: 허용 확장자, 최대 개수, 최대 크기, key prefix
 *
 * 핵심 포인트는 "정규화"다.
 * YAML 값이 비어 있거나 형식이 들쭉날쭉해도, 서비스 계층에서는 이미 정리된 값이라고 믿고 쓸 수 있다.
 */
@ConfigurationProperties(prefix = "media.storage")
public record MediaStorageProperties(
        boolean enabled, // object storage 사용 여부
        String bucket,  // 업로드 대상 bucket 이름
        String region, // S3 region
        String endpoint,
        String publicBaseUrl, // 업로드 후 클라이언트에 노출할 public URL base
        String accessKey, // 명시적으로 지정할 access key
        String secretKey, // 명시적으로 지정할 secret key
        boolean pathStyleAccess, // LocalStack/MinIO 호환을 위한 URL 스타일 옵션
        String keyPrefix, // storage key 앞에 붙는 공통 prefix
        int maxImageCount, // 게시글당 허용할 이미지 최대 개수
        int maxVideoCount, // 게시글당 허용할 비디오 최대 개수
        long maxImageSizeBytes, // 이미지 1개당 최대 크기
        long maxVideoSizeBytes, // 비디오 1개당 최대 크기
        Set<String> allowedImageExtensions, // 허용 이미지 확장자 목록
        Set<String> allowedVideoExtensions  // 허용 비디오 확장자 목록
) {
    public MediaStorageProperties {
        // null/blank/0 같은 값이 들어와도 서비스 코드가 매번 방어하지 않도록
        // "여기서 한 번" 안전한 기본값과 표준 형식으로 정리한다.
        region = hasText(region) ? region.strip() : "ap-northeast-2";
        keyPrefix = hasText(keyPrefix) ? trimSlashes(keyPrefix) : "post-media";
        maxImageCount = maxImageCount > 0 ? maxImageCount : 10;
        maxVideoCount = maxVideoCount > 0 ? maxVideoCount : 1;
        maxImageSizeBytes = maxImageSizeBytes > 0 ? maxImageSizeBytes : 10L * 1024 * 1024;
        maxVideoSizeBytes = maxVideoSizeBytes > 0 ? maxVideoSizeBytes : 100L * 1024 * 1024;
        allowedImageExtensions = normalizeExtensions(
                allowedImageExtensions,
                Set.of("jpg", "jpeg", "png", "gif", "webp")
        );
        allowedVideoExtensions = normalizeExtensions(
                allowedVideoExtensions,
                Set.of("mp4", "mov", "webm")
        );
    }

    private static Set<String> normalizeExtensions(Set<String> source, Set<String> defaults) {
        if (source == null || source.isEmpty()) {
            // 설정이 비어 있으면 서비스가 곧바로 사용할 수 있게 안전한 기본 확장자 집합을 반환한다.
            return defaults;
        }
        // ".jpg", " JPG " 같은 입력 차이를 모두 없애고,
        // 이후 서비스에서는 "소문자 확장자 집합"만 비교하게 만든다.
        return source.stream()
                .filter(MediaStorageProperties::hasText)
                .map(String::strip)
                .map(value -> value.startsWith(".") ? value.substring(1) : value)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimSlashes(String value) {
        // keyPrefix 는 이후 storage key 경로 조합에 그대로 재사용되므로
        // 앞뒤 슬래시를 미리 제거해 "//" 중복이 생기지 않게 한다.
        return value.strip()
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }
}
