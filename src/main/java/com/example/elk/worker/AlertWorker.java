package com.example.elk.worker;

import com.example.elk.domain.Alert;
import com.example.elk.service.AlertQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.alert.worker.enabled", havingValue = "true", matchIfMissing = true)
public class AlertWorker {

    private final AlertQueueService alertQueueService;

    /**
     * 1초마다 Redis 큐를 확인하고 알림 처리
     */
    @Scheduled(fixedDelayString = "${app.alert.worker.poll-interval-ms:1000}")
    public void processAlerts() {
        Alert alert = alertQueueService.pollAlert();

        if (alert != null) {
            handleAlert(alert);
        }
    }

    /**
     * 알림 처리 로직
     */
    private void handleAlert(Alert alert) {
        log.warn("========================================");
        log.warn("🚨 ALERT DETECTED!");
        log.warn("Level: {}", alert.getLevel());
        log.warn("Message: {}", alert.getMessage());
        log.warn("Logger: {}", alert.getLoggerName());
        log.warn("Timestamp: {}", alert.getTimestamp());
        log.warn("Thread: {}", alert.getThreadName());
        if (alert.getStackTrace() != null) {
            log.warn("Stack Trace: {}", alert.getStackTrace());
        }
        log.warn("========================================");

        // TODO: Phase 2에서 이메일, Slack 등으로 확장
    }

    /**
     * 큐 크기 모니터링 (10초마다)
     */
    @Scheduled(fixedDelay = 10000)
    public void monitorQueueSize() {
        Long queueSize = alertQueueService.getQueueSize();
        if (queueSize != null && queueSize > 0) {
            log.info("📊 Alert queue size: {}", queueSize);
        }
    }
}
