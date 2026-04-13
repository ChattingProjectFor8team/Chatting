package com.example.infinite.global.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// 커서 기반 무한 스크롤에서 사용할 slice 조회 크기 정책이다.
public class CursorSliceConfig {

    @Bean
    // 서비스 전역에서 같은 기본/최대 size 정책을 재사용할 수 있게 빈으로 등록한다.
    public CursorSlicePolicy cursorSlicePolicy() {
        return new CursorSlicePolicy(10, 50);
    }

    public record CursorSlicePolicy(int defaultSize, int maxSize) {

        public int normalizeSize(Integer size) {
            // 요청 size가 없거나 비정상이면 기본값을 쓰고, 너무 크면 최대값으로 제한한다.
            if (size == null || size < 1) {
                return defaultSize;
            }
            return Math.min(size, maxSize);
        }
    }
}
