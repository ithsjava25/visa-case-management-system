package org.example.visacasemanagementsystem.visa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.visacasemanagementsystem.visa.VisaStatus;
import org.example.visacasemanagementsystem.visa.VisaType;

public record UpdateVisaDTO(
        @NotNull Long id,
        @NotBlank(message = "Visa type must be specified") VisaType visaType,
        @NotNull(message = "Application must have a status") VisaStatus visaStatus,
        @NotBlank(message = "Nationality must be specified") String nationality,
        Long handlerId //  Handläggaren som uppdaterar visat eller om användaren gör en komplettering?
) {
}
