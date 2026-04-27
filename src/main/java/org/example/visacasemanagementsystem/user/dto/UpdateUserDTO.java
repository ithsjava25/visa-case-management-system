package org.example.visacasemanagementsystem.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateUserDTO(
        @NotNull Long id,
        @NotBlank(message = "Full name must be specified") String fullName,

        @NotNull(message = "Password must be provided (use empty string to skip change)")
        @Pattern(regexp = "^$|.{8,}", message = "Password must be at least 8 characters")
        String password) {
}
