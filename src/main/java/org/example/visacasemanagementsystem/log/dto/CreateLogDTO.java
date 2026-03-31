package org.example.visacasemanagementsystem.log.dto;

import jakarta.validation.constraints.NotNull;
import org.example.visacasemanagementsystem.log.LogEvent;
import java.time.LocalDateTime;

public record CreateLogDTO(
        @NotNull(message = "Log entry must have a time stamp") LocalDateTime timeStamp,
        @NotNull(message = "Log entry must have an associated user id") Long userId,
        @NotNull Long visaCaseId,
        @NotNull(message = "Log entry event type must be specified") LogEvent logEvent,
        String description) {
}
