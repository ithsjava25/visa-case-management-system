package org.example.visacasemanagementsystem.visa.dto;

import java.time.LocalDateTime;

public record CommentDTO(
        Long id,
        Long visaId,
        String authorName,
        String text,
        LocalDateTime createdAt
) {
}
