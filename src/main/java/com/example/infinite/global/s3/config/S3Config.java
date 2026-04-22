package com.example.infinite.global.s3.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.s3.InMemoryBufferingS3OutputStreamProvider;
import io.awspring.cloud.s3.S3ObjectConverter;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.InputStream;


// s3자동설정을 꺼버려서 bean등록을 해줘야함
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

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }




    // 👈 이 부분을 새로 추가하세요!
    @Bean
    public S3Template s3Template(S3Client s3Client) {
        // 1. 컨버터 구현 (인터페이스 규격에 완벽 일치)
        S3ObjectConverter objectConverter = new S3ObjectConverter() {
            @Override
            public <T> RequestBody write(T object) { // 👈 인자 1개인 경우
                try {
                    return RequestBody.fromBytes(objectMapper.writeValueAsBytes(object));
                } catch (Exception e) {
                    throw new RuntimeException("S3 Serialization Failed", e);
                }
            }

            @Override
            public <T> T read(InputStream is, Class<T> clazz) { // 👈 인자 2개인 경우
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

        // 2. 최종 조립
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
