package com.example.infinite.global.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
// 페이지 번호 기반 조회에서 사용할 Pageable 기본값을 전역으로 맞춘다.
public class PageableConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer customize() {
        return resolver -> {
            resolver.setFallbackPageable(PageRequest.of(0, 10));
            resolver.setOneIndexedParameters(true);
        };
    }
}
