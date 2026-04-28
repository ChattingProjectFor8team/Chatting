package com.example.infinite.global.auth;


import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * 인가(Authorization) 실패 시 실행되는 핸들러 (403 Forbidden)
 * 인증은 되었으나 해당 자원에 접근할 권한(Role)이 없을 때 호출됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;


    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        log.warn("권한 없는 사용자의 접근: {}", accessDeniedException.getMessage());


        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;

        // 1. 응답 설정 (JSON, 403 Forbidden)
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // errorCode.getStatus().value()를 통해 403
        response.setStatus(errorCode.getStatus().value());

        // 2. 프로젝트 표준 에러 응답 생성
        ApiResponse<Void> errorResponse = ApiResponse.fail(
                errorCode.getCode(),
                errorCode.getMessage()
        );

        // 3. JSON 변환 후 출력
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}
