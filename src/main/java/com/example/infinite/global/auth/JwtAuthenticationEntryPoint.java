package com.example.infinite.global.auth;

import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.ErrorResponse;
import com.example.infinite.global.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 사용자가 인증 없이 보호된 리소스에 접근했을 때 실행되는 엔트리 포인트
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // 코드 설명 : 인증되지 않은 사용자가 인증이 필요한 엔드포인트에 접근하려고 할 때 발생한 예외를 잡아서 JSON 형태의 API 스펙으로 응답
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("인증 에러 발생: {}", authException.getMessage());
        log.error("Request Uri : {}", request.getRequestURI());

        // C001(인증 필요) 에러 코드를 사용 <- 필요할 거 같음
        ErrorCode errorCode = ErrorCode.INVALID_AUTHENTICATION;

        // 표준 에러 응답 구조를 생성
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.getStatus().name())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .build();

        // ApiResponse로 처리
        ApiResponse<Void> apiResponse = ApiResponse.fail(errorResponse);

        // 클라이언트에 JSON 형식으로 응답 진행
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(errorCode.getStatus().value());
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
