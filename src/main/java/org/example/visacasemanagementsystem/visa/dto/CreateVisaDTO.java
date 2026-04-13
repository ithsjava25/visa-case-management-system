package org.example.visacasemanagementsystem.visa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.visacasemanagementsystem.visa.VisaType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record CreateVisaDTO(
        @NotNull(message = "Visa type must be specified") VisaType visaType,
        @NotBlank(message = "Nationality must be specified") String nationality,
        @NotBlank(message = "Passport number must be provided") String passportNumber,
        @NotNull(message = "Travel date must be provided") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
        Long applicantId
) {
}
