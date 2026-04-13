package com.example.infinite.global.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// 커서 기반 무한 스크롤에서 사용할 slice 조회 크기 정책이다.
public class CursorSliceConfig {

    @Bean
    public CursorSlicePolicy cursorSlicePolicy() {
        return new CursorSlicePolicy(10, 50);
    }

    public record CursorSlicePolicy(int defaultSize, int maxSize) {

        public int normalizeSize(Integer size) {
            if (size == null || size < 1) {
                return defaultSize;
            }
            return Math.min(size, maxSize);
        }
    }
}
