package org.example.visacasemanagementsystem.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.visacasemanagementsystem.user.UserAuthorization;

public record CreateUserDTO(
        @NotBlank(message = "Full name must be specified") String fullName,
        @NotBlank(message = "Email must be specified") String email,
        @NotBlank String password,
        @NotNull(message = "Authorization level must be selected") UserAuthorization userAuthorization) {
}
