package org.example.visacasemanagementsystem.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCommentDTO(
        @NotNull(message = "Visa ID is required") Long visaId,
        @NotBlank(message = "Comment text cannot be empty") String  text
        ) {
}
