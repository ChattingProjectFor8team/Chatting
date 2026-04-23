package com.example.infinite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConfigurationPropertiesScan
@EnableScheduling
@SpringBootApplication(exclude = {
        // S3 자동 설정 제외
        io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration.class,
        // 파라미터 스토어 및 환경설정 로드 기능 제외
        io.awspring.cloud.autoconfigure.config.parameterstore.ParameterStoreAutoConfiguration.class
})
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
