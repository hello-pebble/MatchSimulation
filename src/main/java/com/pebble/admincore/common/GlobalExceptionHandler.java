package com.pebble.admincore.common;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException e) {
        return ResponseEntity.status(e.getStatus())
                .body(Map.of("status", e.getStatus().value(), "message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest()
                .body(Map.of("status", HttpStatus.BAD_REQUEST.value(), "message", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("status", HttpStatus.BAD_REQUEST.value(), "message", e.getMessage()));
    }

    /** 낙관적 락 충돌 — 동시에 같은 데이터를 수정하려 한 경우 */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(OptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("status", HttpStatus.CONFLICT.value(),
                        "message", "다른 요청이 먼저 처리되었습니다. 새로고침 후 다시 시도하세요."));
    }

    /** 허용되지 않은 sort 필드 등 잘못된 페이징 파라미터 */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidSort(PropertyReferenceException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("status", HttpStatus.BAD_REQUEST.value(),
                        "message", "정렬할 수 없는 필드입니다: " + e.getPropertyName()));
    }
}
