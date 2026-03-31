package org.example.visacasemanagementsystem.visa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.visacasemanagementsystem.visa.VisaStatus;

public record CreateVisaDTO(
        @NotBlank(message = "Visa type must be specified") String visaType,
        @NotBlank(message = "Nationality must be specified") String nationality,
        @NotNull Long applicantId // Användaren som skapar ärendet
) {
}
