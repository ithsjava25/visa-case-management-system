package org.example.visacasemanagementsystem.visa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.visacasemanagementsystem.visa.VisaStatus;

public record UpdateVisaDTO(
        @NotNull Long id,
        @NotBlank(message = "Visa type must be specified") String visa,
        @NotNull(message = "Application must have a status") VisaStatus visaStatus,
        @NotBlank(message = "Nationality must be specified") String nationality) {
}
