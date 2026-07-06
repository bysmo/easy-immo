package com.easyimmo.tenant.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        log.error("Exception non gérée interceptée par le handler global", ex);
        
        // Obtenir le stacktrace sous forme de String
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String stackTrace = sw.toString();
        
        Map<String, Object> body = Map.of(
            "timestamp", java.time.LocalDateTime.now().toString(),
            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "error", "Internal Server Error",
            "message", ex.getMessage() != null ? ex.getMessage() : ex.toString(),
            "trace", stackTrace
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
