package org.example.visacasemanagementsystem.audit.dto;

import org.example.visacasemanagementsystem.audit.FileEventType;

import java.time.LocalDateTime;

public record FileLogDTO(
        Long id,
        LocalDateTime timeStamp,
        Long actorUserId,
        Long visaCaseId,
        String fileName,
        FileEventType fileEventType,
        String description){
}
