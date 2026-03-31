package org.example.visacasemanagementsystem.log.dto;

import org.example.visacasemanagementsystem.log.LogEvent;
import java.time.LocalDateTime;

public record LogDTO (
        Long id,
        LocalDateTime timeStamp,
        Long userId,
        Long visaCaseId,
        LogEvent logEvent,
        String description) {
}
