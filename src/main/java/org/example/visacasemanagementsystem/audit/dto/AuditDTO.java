package org.example.visacasemanagementsystem.audit.dto;

import org.example.visacasemanagementsystem.audit.AuditEventType;

import java.time.LocalDateTime;

public record AuditDTO(
        Long id,
        LocalDateTime timeStamp,
        Long userId,
        Long visaCaseId,
        AuditEventType auditEventType,
        String description) {
}
