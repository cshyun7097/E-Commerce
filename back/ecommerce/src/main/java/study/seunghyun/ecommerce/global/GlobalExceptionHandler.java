package study.seunghyun.ecommerce.global;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> business(BusinessException e) {
        return fail(e.getErrorCode());
    }

    // 아이디 없음/비밀번호 틀림/탈퇴 회원을 구분하지 않음 (계정 존재 여부 노출 방지)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> unauthorized(AuthenticationException e) {
        return fail(ErrorCode.LOGIN_FAILED);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> locked(LockedException e) {
        return fail(ErrorCode.ACCOUNT_LOCKED);
    }

    // 로그인 중 DB 장애 등 내부 오류. 비밀번호 틀림으로 숨기지 않고 500으로 응답
    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ApiResponse<Void>> authInternalError(InternalAuthenticationServiceException e) {
        log.error("Authentication internal error", e);
        return fail(ErrorCode.INTERNAL_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> forbidden(AccessDeniedException e) {
        return fail(ErrorCode.FORBIDDEN);
    }

    // 예상하지 못한 오류는 내부 정보를 숨기고 로그로만 남김
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
        log.error("Unexpected error", e);
        return fail(ErrorCode.INTERNAL_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(status).headers(headers)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.name(), message));
    }

    // 그 외 Spring MVC 기본 예외(잘못된 JSON, 지원하지 않는 메서드 등)도 공통 응답으로 변환
    // 400은 INVALID_INPUT 코드와 한글 메시지, 나머지는 HTTP 상태 이름(METHOD_NOT_ALLOWED 등)과 Spring 기본 메시지
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        if (status == HttpStatus.BAD_REQUEST) {
            return ResponseEntity.status(statusCode).headers(headers).body(ApiResponse.fail(ErrorCode.INVALID_INPUT));
        }
        String message = body instanceof ProblemDetail problem && problem.getDetail() != null
                ? problem.getDetail() : status.getReasonPhrase();
        return ResponseEntity.status(statusCode).headers(headers).body(ApiResponse.fail(status.name(), message));
    }

    private ResponseEntity<ApiResponse<Void>> fail(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.fail(errorCode));
    }
}
