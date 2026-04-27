package org.example.visacasemanagementsystem.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserDTO(
        @NotNull Long id,
        @NotBlank(message = "Full name must be specified") String fullName,
        @NotBlank(message = "Password must be set") String password) {
}
