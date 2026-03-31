package org.example.visacasemanagementsystem.visa.dto;

import org.example.visacasemanagementsystem.visa.VisaStatus;

public record VisaDTO(
        Long id,
        String visa,
        VisaStatus visaStatus,
        String nationality) {
}
