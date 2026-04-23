package org.example.visacasemanagementsystem.audit.dto;

import org.example.visacasemanagementsystem.audit.UserEventType;

import java.time.LocalDateTime;

public record UserLogDTO(
        Long id,
        LocalDateTime timeStamp,
        Long actorUserId,
        Long targetUserId,
        UserEventType userEventType,
        String description) {
}
