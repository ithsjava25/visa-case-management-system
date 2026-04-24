package org.example.visacasemanagementsystem.audit.dto;

import org.example.visacasemanagementsystem.audit.CommentEventType;

import java.time.LocalDateTime;

public record CommentLogDTO(
        Long id,
        LocalDateTime timeStamp,
        Long actorUserId,
        Long visaCaseId,
        Long commentId,
        CommentEventType commentEventType,
        String description) {
}
