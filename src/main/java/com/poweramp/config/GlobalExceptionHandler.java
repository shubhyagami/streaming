package com.poweramp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("Unhandled exception: {}", e.getMessage());
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "Internal server error";
        }
        return ResponseEntity.internalServerError().body(Map.of("error", msg, "message", msg));
    }
}
