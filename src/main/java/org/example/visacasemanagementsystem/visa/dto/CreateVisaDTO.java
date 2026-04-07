package org.example.visacasemanagementsystem.visa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.visacasemanagementsystem.visa.VisaType;

public record CreateVisaDTO(
        @NotNull(message = "Visa type must be specified") VisaType visaType,
        @NotBlank(message = "Nationality must be specified") String nationality,
        @NotNull Long applicantId // Användaren som ansöker om visa
) {
}
