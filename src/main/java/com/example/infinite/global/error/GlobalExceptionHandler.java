package com.example.infinite.global.error;

import com.example.infinite.domain.ArtistContent.error.ArtistContentException;
import com.example.infinite.domain.Interaction.error.InteractionException;
import com.example.infinite.domain.Member.error.ArtistException;
import com.example.infinite.domain.Member.error.MemberErrorCode;
import com.example.infinite.domain.Member.error.MemberException;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
// 애플리케이션 전역 예외를 공통 응답 형식으로 변환한다.
public class GlobalExceptionHandler {

    /**
     * 400: 잘못된 요청을 공통 에러 응답으로 반환
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("IllegalArgumentException : {}", e.getMessage());
        return ResponseEntity
                .badRequest() // 400 Bad Request
                .body(ApiResponse.fail(buildErrorResponse(ErrorCode.INVALID_INPUT_VALUE, e.getMessage(), request.getRequestURI())));
    }

    /**
     * 404: 리소스가 존재하지 않는 경우 공통 에러 응답으로 반환
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException e, HttpServletRequest request) {
        log.warn("IllegalStateException : {}", e.getMessage());
        ErrorCodeType errorCode = MemberErrorCode.MEMBER_NOT_FOUND;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(buildErrorResponse(errorCode, e.getMessage(), request.getRequestURI())));
    }

    /**
     * @Valid 어노테이션을 통한 입력값 검증 실패 시 발생하는 예외를 처리합니다.
     * 예: 비밀번호가 정규표현식에 맞지 않거나, 필수값이 누락된 경우
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("MethodArgumentNotValidException : {}", e.getMessage());
        // 검증 실패 메시지 중 첫 번째 메시지를 가져옵니다.
        String errorMessage = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity
                .badRequest() // 400 Bad Request
                .body(ApiResponse.fail(buildErrorResponse(ErrorCode.INVALID_INPUT_VALUE, errorMessage, request.getRequestURI())));
    }

    /**
     * 400: QueryParam/pathVariable Bean Validation 실패(@Min/@Max) 시
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        log.warn("ConstraintViolationException : {}", e.getMessage());
        // 검증 실패 메시지 중 첫 번째 메시지를 가져옵니다.
        String errorMessage = e.getConstraintViolations()
                .stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse("요청 값이 유효하지 않음");
        return ResponseEntity
                .badRequest() // 400 Bad Request
                .body(ApiResponse.fail(buildErrorResponse(ErrorCode.INVALID_INPUT_VALUE, errorMessage, request.getRequestURI())));
    }

    /**
     * 401: 인증되지 않은 요청(세션이 없을 때 등)에 대해 로그인 필요 응답 반환
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request) {
        // 인증 관련 커스텀 예외는 예외 내부의 ErrorCode를 그대로 응답에 사용한다.
        return handleCustomException("UnauthorizedException", e.getErrorCode(), e, request);
    }

    /**
     * 4xx/5xx: 회원 도메인 커스텀 예외 처리
     */
    @ExceptionHandler(MemberException.class)
    public ResponseEntity<ApiResponse<Void>> handleMemberException(MemberException e, HttpServletRequest request) {
        return handleCustomException("MemberException", e.getErrorCode(), e, request);
    }

    /**
     * 4xx/5xx: 일반 사용자 도메인 커스텀 예외 처리
     */
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserException(UserException e, HttpServletRequest request) {
        return handleCustomException("UserException", e.getErrorCode(), e, request);
    }

    /**
     * 4xx/5xx: 아티스트 도메인 커스텀 예외 처리
     */
    @ExceptionHandler(ArtistException.class)
    public ResponseEntity<ApiResponse<Void>> handleArtistException(ArtistException e, HttpServletRequest request) {
        return handleCustomException("ArtistException", e.getErrorCode(), e, request);
    }

    /**
     * 4xx/5xx: 아티스트 콘텐츠 도메인 커스텀 예외 처리
     */
    @ExceptionHandler(ArtistContentException.class)
    public ResponseEntity<ApiResponse<Void>> handleArtistContentException(ArtistContentException e, HttpServletRequest request) {
        return handleCustomException("ArtistContentException", e.getErrorCode(), e, request);
    }

    /**
     * 4xx/5xx: DM 도메인 커스텀 예외 처리
     */
    @ExceptionHandler(DMException.class)
    public ResponseEntity<ApiResponse<Void>> handleDMException(DMException e, HttpServletRequest request) {
        return handleCustomException("DMException", e.getErrorCode(), e, request);
    }

    /**
     * 4xx/5xx: 커뮤니티 상호작용 도메인 커스텀 예외 처리
     */
    @ExceptionHandler(InteractionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInteractionException(InteractionException e, HttpServletRequest request) {
        return handleCustomException("InteractionException", e.getErrorCode(), e, request);
    }

    /**
     * 4xx/5xx: 결제 도메인 커스텀 예외 처리
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentException(PaymentException e, HttpServletRequest request) {
        return handleCustomException("PaymentException", e.getErrorCode(), e, request);
    }

    /**
     * 4xx/5xx: 래플 도메인 커스텀 예외 처리
     */
    @ExceptionHandler(RaffleException.class)
    public ResponseEntity<ApiResponse<Void>> handleRaffleException(RaffleException e, HttpServletRequest request) {
        return handleCustomException("RaffleException", e.getErrorCode(), e, request);
    }

    /**
     * 4xx/5xx: 라이브 도메인 커스텀 예외 처리
     */
    @ExceptionHandler(RealtimeLiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleRealtimeLiveException(RealtimeLiveException e, HttpServletRequest request) {
        return handleCustomException("RealtimeLiveException", e.getErrorCode(), e, request);
    }

    /**
     * 4xx/5xx: 구독/멤버십 도메인 커스텀 예외 처리
     */
    @ExceptionHandler(SubscriptionMembershipException.class)
    public ResponseEntity<ApiResponse<Void>> handleSubscriptionMembershipException(SubscriptionMembershipException e, HttpServletRequest request) {
        return handleCustomException("SubscriptionMembershipException", e.getErrorCode(), e, request);
    }

    /**
     * 403: 권한이 없는 요청(인가 실패) 처리
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDeniedException(
            AuthorizationDeniedException e,
            HttpServletRequest request
    ) {
        log.warn("AuthorizationDeniedException : {}", e.getMessage());
        ErrorCodeType errorCode = ErrorCode.ACCESS_DENIED;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(buildErrorResponse(errorCode, errorCode.getMessage(), request.getRequestURI())));
    }

    /**
     * 위에서 처리하지 못한 나머지 모든 예외(Exception)를 처리합니다.
     * 예상치 못한 서버 에러(NullPointerException 등)가 여기에 해당합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("Exception : {}", e.getMessage());
        // 보안을 위해 내부 에러 메시지는 숨기고, "서버 내부 오류"라는 일반적인 메시지를 반환합니다.
        return ResponseEntity
                .internalServerError() // 500 Internal Server Error
                .body(ApiResponse.fail(buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "알 수 없는 에러가 발생했습니다.", request.getRequestURI())));
    }

    /**
     * ErrorResponse 객체를 생성하는 유틸리티 메서드입니다.
     */
    private ErrorResponse buildErrorResponse(ErrorCodeType errorCode, String message, String path) {
        // 컨트롤러 밖에서도 동일한 에러 응답 구조를 유지하기 위한 조립 메서드다.
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.getStatus().name())
                .code(errorCode.getCode())
                .message(message)
                .path(path)
                .build();
    }

    /**
     * ErrorCode를 가진 커스텀 예외는 이 메서드로 공통 응답 형식을 맞춘다.
     */
    private ResponseEntity<ApiResponse<Void>> handleCustomException(
            String exceptionName,
            ErrorCodeType errorCode,
            RuntimeException e,
            HttpServletRequest request
    ) {
        log.warn("{} : {}", exceptionName, e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(buildErrorResponse(errorCode, e.getMessage(), request.getRequestURI())));
    }
}

