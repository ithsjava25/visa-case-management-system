package org.example.visacasemanagementsystem.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.visacasemanagementsystem.user.UserAuthorization;

public record CreateUserDTO(
        @NotBlank(message = "Full name must be specified") String fullName,
        @NotBlank(message = "Email must be specified") String email,
        @Size(min = 8, message = "Password must be at least 8 characters") @NotBlank String password,
        @NotNull(message = "Authorization level must be selected") UserAuthorization userAuthorization) {
}
