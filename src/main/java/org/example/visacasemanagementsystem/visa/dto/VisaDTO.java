package org.example.visacasemanagementsystem.visa.dto;

import org.example.visacasemanagementsystem.visa.VisaStatus;

public record VisaDTO(
        Long id,
        String visaType,
        VisaStatus visaStatus,
        String nationality,
        Long applicantId,
        String applicantName,
        Long handlerId
) {
}
