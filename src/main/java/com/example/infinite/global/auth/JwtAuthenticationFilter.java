package com.example.infinite.global.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

        // 2. 토큰 유효성 검사 및 인증 처리
        if (StringUtils.hasText(jwt)) {
            try {
                // [수정 포인트] 이제 boolean 리턴이 아니라 Claims를 가져오거나 예외가 터집니다.
                Claims claims = jwtTokenProvider.validateToken(jwt);

                // 3. 유효시 인증 객체 생성 (이미 검증된 claims가 있다면 더 효율적입니다)
                Authentication authentication = jwtTokenProvider.getAuthentication(jwt);

                // 4. SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (ExpiredJwtException e) {
                log.warn("만료된 토큰 요청입니다: {}", e.getMessage());
                // TODO : 필요 시 request.setAttribute("exception", ErrorCode.TOKEN_EXPIRED); 추가
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("유효하지 않은 토큰 요청입니다: {}", e.getMessage());
                // TODO : 필요 시 request.setAttribute("exception", ErrorCode.INVALID_TOKEN); 추가
            }
        }

        // 5. 다음 필터로 진행
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
