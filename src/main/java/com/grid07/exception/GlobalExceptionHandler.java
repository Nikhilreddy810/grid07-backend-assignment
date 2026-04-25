package com.grid07.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BotCapExceededException.class)
    public ResponseEntity<Map<String, Object>> handleBotCap(BotCapExceededException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(BotCooldownException.class)
    public ResponseEntity<Map<String, Object>> handleCooldown(BotCooldownException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(VerticalCapExceededException.class)
    public ResponseEntity<Map<String, Object>> handleVertical(VerticalCapExceededException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(RuntimeException ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", status.value(),
            "error", message
        ));
    }
}
