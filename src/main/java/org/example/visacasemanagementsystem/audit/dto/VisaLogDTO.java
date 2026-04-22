package org.example.visacasemanagementsystem.audit.dto;

import org.example.visacasemanagementsystem.audit.VisaEventType;

import java.time.LocalDateTime;

public record VisaLogDTO(
        Long id,
        LocalDateTime timeStamp,
        Long userId,
        Long visaCaseId,
        VisaEventType visaEventType,
        String description) {
}
