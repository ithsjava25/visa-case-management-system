package org.example.visacasemanagementsystem.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCommentDTO(
        @NotNull(message = "Visa ID is required") Long visaId,
        @NotNull(message = "Author ID is required") Long authorId,
        @NotBlank(message = "Comment text cannot be empty") String  text
        ) {
}
