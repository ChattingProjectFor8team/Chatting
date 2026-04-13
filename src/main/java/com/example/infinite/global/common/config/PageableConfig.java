package com.example.infinite.global.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
// Pageable 기본값과 페이지 번호 규칙을 프로젝트 전역에서 통일한다.
public class PageableConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer customize() {
        return resolver -> {
            // page, size 파라미터가 없으면 1페이지당 10개를 기본값으로 사용한다.
            resolver.setFallbackPageable(PageRequest.of(0, 10));
            // 클라이언트는 1페이지부터 요청하고, 내부에서는 0페이지로 변환해 처리한다.
            resolver.setOneIndexedParameters(true);
        };
    }
}
