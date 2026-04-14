package com.example.infinite.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 모든 요청에서 JWT 유효성을 검사하는 필터
 * OncePerRequestFilter를 상속받아 한 요청당 딱 한 번만 실행됨을 보장합니다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 요청 헤더에서 토큰 추출
        String jwt = resolveToken(request);

        // 2. 토큰 유효성 검사 (validateToken 활용)
        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
            // 3. 유효시 인증 객체 생성
            Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
            // 4. SecurityContext에 인증 정보 저장 (이게 되어야 '로그인 상태'로 인정됨)
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 5. 필터 진행
        filterChain.doFilter(request, response);
    }

    // 헤더에서 "Bearer "를 제외한 순수 토큰 문자열만 꺼내오는 로직
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
