package com.example.elk.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    private String message;
    private String level;
    private String loggerName;
    private LocalDateTime timestamp;
    private String threadName;
    private String stackTrace;
}
