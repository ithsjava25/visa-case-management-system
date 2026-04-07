package org.example.visacasemanagementsystem.user.dto;

import org.example.visacasemanagementsystem.user.UserAuthorization;

public record UserDTO(
        Long id,
        String fullName,
        String email,
        UserAuthorization userAuthorization) {
}
