package com.example.elk.controller;

import com.example.elk.service.AlertQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "Alert Monitor API", description = "알림 큐 모니터링 및 Redis 상태 확인 API")
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class AlertMonitorController {

    private final AlertQueueService alertQueueService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Operation(
            summary = "알림 큐 상태 조회",
            description = "Redis 큐에 쌓여있는 알림의 개수와 상태를 조회합니다."
    )
    @GetMapping("/queue/status")
    public Map<String, Object> getQueueStatus() {
        Long queueSize = alertQueueService.getQueueSize();

        Map<String, Object> status = new HashMap<>();
        status.put("queueName", "alert:queue");
        status.put("queueSize", queueSize != null ? queueSize : 0);
        status.put("isEmpty", queueSize == null || queueSize == 0);
        status.put("message", queueSize != null && queueSize > 0
                ? "큐에 " + queueSize + "개의 알림이 대기 중입니다."
                : "큐가 비어있습니다.");

        return status;
    }

    @Operation(
            summary = "알림 큐 미리보기",
            description = "큐에서 제거하지 않고 앞의 5개 알림을 미리 확인합니다."
    )
    @GetMapping("/queue/peek")
    public Map<String, Object> peekQueue() {
        List<Object> items = redisTemplate.opsForList().range("alert:queue", 0, 4);

        Map<String, Object> result = new HashMap<>();
        result.put("count", items != null ? items.size() : 0);
        result.put("items", items);

        return result;
    }

    @Operation(
            summary = "Redis 모든 키 조회",
            description = "Redis에 저장된 모든 키를 조회합니다."
    )
    @GetMapping("/redis/keys")
    public Map<String, Object> getAllKeys() {
        Set<String> keys = redisTemplate.keys("*");

        Map<String, Object> result = new HashMap<>();
        result.put("totalKeys", keys != null ? keys.size() : 0);
        result.put("keys", keys);

        return result;
    }

    @Operation(
            summary = "Redis 연결 상태 확인",
            description = "Redis 서버와의 연결 상태를 확인합니다."
    )
    @GetMapping("/redis/ping")
    public Map<String, String> pingRedis() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();

            return Map.of(
                    "status", "connected",
                    "response", pong != null ? pong : "PONG",
                    "message", "Redis 연결 정상"
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "message", "Redis 연결 실패: " + e.getMessage()
            );
        }
    }

    @Operation(
            summary = "전체 시스템 상태 조회",
            description = "Redis 연결, 큐 상태 등 전체 시스템 상태를 한 번에 조회합니다."
    )
    @GetMapping("/health")
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();

        // Redis 연결 상태
        try {
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            health.put("redis", Map.of(
                    "status", "UP",
                    "response", pong != null ? pong : "PONG"
            ));
        } catch (Exception e) {
            health.put("redis", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
            ));
        }

        // 큐 상태
        Long queueSize = alertQueueService.getQueueSize();
        health.put("alertQueue", Map.of(
                "size", queueSize != null ? queueSize : 0,
                "status", queueSize != null && queueSize < 100 ? "HEALTHY" : "WARNING"
        ));

        // 전체 키 개수
        Set<String> keys = redisTemplate.keys("*");
        health.put("totalRedisKeys", keys != null ? keys.size() : 0);

        return health;
    }
}
