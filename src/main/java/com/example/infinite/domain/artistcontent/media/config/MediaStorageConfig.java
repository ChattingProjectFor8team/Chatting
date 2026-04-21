package com.example.infinite.domain.artistcontent.media.config;

import com.example.infinite.domain.artistcontent.media.storage.DisabledObjectStorageClient;
import com.example.infinite.domain.artistcontent.media.storage.ObjectStorageClient;
import com.example.infinite.domain.artistcontent.media.storage.S3ObjectStorageClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/*
 * MediaStorageProperties 설명서를 읽고 실제 Bean 을 조립하는 설정 클래스다.
 *
 * 런타임에서는 항상 ObjectStorageClient 가 하나 존재하도록 만든다.
 * - media.storage.enabled=true  -> S3ObjectStorageClient 등록
 * - 그 외                      -> DisabledObjectStorageClient 등록
 *
 * 덕분에 MediaService 는 "지금 S3가 켜져 있는가"를 직접 분기하지 않고
 * ObjectStorageClient 인터페이스만 의존하면 된다.
 */
@Configuration
public class MediaStorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "media.storage", name = "enabled", havingValue = "true")
    public S3Client s3Client(MediaStorageProperties properties) {
        // 저수준 AWS SDK 클라이언트를 조립하는 단계다.
        // AWS S3 뿐 아니라 LocalStack/MinIO 같은 S3 호환 스토리지도 붙일 수 있게
        // endpoint/path-style 옵션을 설정값으로 분리해 두었다.
        var builder = S3Client.builder()
                .region(Region.of(properties.region()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.pathStyleAccess())
                        .build());

        if (properties.accessKey() != null && !properties.accessKey().isBlank()
                && properties.secretKey() != null && !properties.secretKey().isBlank()) {
            // access key / secret key 를 명시한 환경에서는 그 값을 우선 사용한다.
            // 둘 다 비어 있으면 AWS SDK 기본 credential chain 에 맡긴다.
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())
            ));
        }

        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            // 운영 AWS 기본 endpoint 대신 별도 endpoint 를 쓰는 경우(LocalStack/MinIO 등)만 override 한다.
            builder.endpointOverride(URI.create(properties.endpoint()));
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "media.storage", name = "enabled", havingValue = "true")
    public ObjectStorageClient objectStorageClient(S3Client s3Client, MediaStorageProperties properties) {
        // 서비스 계층은 S3 SDK 자체를 직접 알 필요가 없으므로
        // "S3Client -> ObjectStorageClient 어댑터"를 여기서 감싸서 주입한다.
        return new S3ObjectStorageClient(s3Client, properties);
    }

    @Bean
    @ConditionalOnMissingBean(ObjectStorageClient.class)
    public ObjectStorageClient disabledObjectStorageClient() {
        // 로컬 기본 실행은 S3 없이도 가능해야 하므로 앱 부팅 자체는 막지 않는다.
        // 대신 업로드/삭제를 시도한 순간 명확한 예외를 던지는 fallback 구현체를 둔다.
        return new DisabledObjectStorageClient();
    }
}
