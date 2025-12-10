package com.example.elk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Log API", description = "로그 생성 및 테스트 API")
@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    @Operation(
            summary = "로그 생성",
            description = "지정한 레벨(DEBUG, INFO, WARN, ERROR)의 로그를 생성합니다. ERROR 로그는 자동으로 알림이 발송됩니다."
    )
    @PostMapping("/test/{level}")
    public Map<String, String> createLog(
            @Parameter(description = "로그 레벨 (DEBUG, INFO, WARN, ERROR)", example = "ERROR")
            @PathVariable String level,
            @Parameter(description = "로그 메시지", example = "Database connection failed")
            @RequestParam(defaultValue = "Test log message") String message) {

        switch (level.toUpperCase()) {
            case "DEBUG":
                log.debug("DEBUG: {}", message);
                break;
            case "INFO":
                log.info("INFO: {}", message);
                break;
            case "WARN":
                log.warn("WARN: {}", message);
                break;
            case "ERROR":
                log.error("ERROR: {}", message);
                break;
            default:
                log.info("Unknown level, logging as INFO: {}", message);
        }

        return Map.of(
                "status", "success",
                "level", level,
                "message", message
        );
    }

    @Operation(
            summary = "예외 포함 ERROR 로그 생성",
            description = "예외를 발생시켜 스택 트레이스가 포함된 ERROR 로그를 생성합니다."
    )
    @PostMapping("/error")
    public Map<String, String> createErrorWithException(
            @Parameter(description = "에러 메시지", example = "Payment processing error")
            @RequestParam(defaultValue = "Simulated error") String message) {
        try {
            // 의도적으로 예외 발생
            throw new RuntimeException(message);
        } catch (Exception e) {
            log.error("Caught exception: {}", message, e);
            return Map.of(
                    "status", "error_logged",
                    "message", message
            );
        }
    }

    @Operation(
            summary = "대량 로그 생성",
            description = "지정한 개수만큼 로그를 연속으로 생성합니다. 부하 테스트 및 알림 임계치 테스트에 사용됩니다."
    )
    @PostMapping("/bulk")
    public Map<String, Object> createBulkLogs(
            @Parameter(description = "로그 레벨", example = "ERROR")
            @RequestParam(defaultValue = "ERROR") String level,
            @Parameter(description = "생성할 로그 개수", example = "10")
            @RequestParam(defaultValue = "10") int count) {

        for (int i = 1; i <= count; i++) {
            String message = String.format("Bulk log #%d", i);

            switch (level.toUpperCase()) {
                case "DEBUG":
                    log.debug(message);
                    break;
                case "INFO":
                    log.info(message);
                    break;
                case "WARN":
                    log.warn(message);
                    break;
                case "ERROR":
                    log.error(message);
                    break;
            }
        }

        return Map.of(
                "status", "success",
                "level", level,
                "count", count
        );
    }

    @Operation(
            summary = "패턴 기반 로그 생성",
            description = "특정 패턴의 ERROR 로그를 생성합니다. 알림 규칙 테스트에 사용됩니다."
    )
    @PostMapping("/pattern")
    public Map<String, String> createPatternLog(
            @Parameter(description = "로그 패턴", example = "DATABASE_ERROR")
            @RequestParam String pattern) {
        log.error("PATTERN_ALERT: {}", pattern);
        return Map.of(
                "status", "success",
                "pattern", pattern
        );
    }
}
