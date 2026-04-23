package com.example.infinite.global.s3.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.s3.InMemoryBufferingS3OutputStreamProvider;
import io.awspring.cloud.s3.S3ObjectConverter;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.InputStream;

@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final ObjectMapper objectMapper;

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    // S3Client가 활성화될 때(enabled=true)만 S3Template도 생성
    @Bean
    @ConditionalOnProperty(prefix = "media.storage", name = "enabled", havingValue = "true")
    public S3Template s3Template(S3Client s3Client) {
        // 1. 컨버터 구현
        S3ObjectConverter objectConverter = new S3ObjectConverter() {
            @Override
            public <T> RequestBody write(T object) {
                try {
                    return RequestBody.fromBytes(objectMapper.writeValueAsBytes(object));
                } catch (Exception e) {
                    throw new RuntimeException("S3 Serialization Failed", e);
                }
            }

            @Override
            public <T> T read(InputStream is, Class<T> clazz) {
                try {
                    return objectMapper.readValue(is, clazz);
                } catch (Exception e) {
                    throw new RuntimeException("S3 Deserialization Failed", e);
                }
            }

            @Override
            public String contentType() {
                return "application/json";
            }
        };

        return new S3Template(
                s3Client,
                new InMemoryBufferingS3OutputStreamProvider(s3Client, null),
                objectConverter,
                S3Presigner.builder()
                        .region(Region.of(region))
                        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                        .build()
        );
    }
}
