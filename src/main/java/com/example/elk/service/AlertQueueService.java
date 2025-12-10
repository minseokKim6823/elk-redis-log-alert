package com.example.elk.service;

import com.example.elk.domain.Alert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.alert.queue-name}")
    private String queueName;

    /**
     * Redis 큐에서 알림을 하나 꺼내옴 (LPOP)
     */
    public Alert pollAlert() {
        try {
            Object result = redisTemplate.opsForList().leftPop(queueName, 1, TimeUnit.SECONDS);
            if (result == null) {
                return null;
            }

            // Redis에서 가져온 데이터를 Alert 객체로 변환
            String json = objectMapper.writeValueAsString(result);
            return objectMapper.readValue(json, Alert.class);
        } catch (Exception e) {
            log.error("Failed to poll alert from queue", e);
            return null;
        }
    }

    /**
     * 큐에 남은 알림 개수 확인
     */
    public Long getQueueSize() {
        return redisTemplate.opsForList().size(queueName);
    }

    /**
     * 수동으로 알림을 큐에 추가 (테스트용)
     */
    public void pushAlert(Alert alert) {
        try {
            String json = objectMapper.writeValueAsString(alert);
            redisTemplate.opsForList().rightPush(queueName, json);
            log.debug("Alert pushed to queue: {}", alert.getMessage());
        } catch (JsonProcessingException e) {
            log.error("Failed to push alert to queue", e);
        }
    }
}
