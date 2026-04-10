package org.example.visacasemanagementsystem.visa.dto;

import org.example.visacasemanagementsystem.visa.VisaStatus;
import org.example.visacasemanagementsystem.visa.VisaType;

import java.time.LocalDateTime;

public record VisaDTO(
        Long id,
        VisaType visaType,
        VisaStatus visaStatus,
        String nationality,
        Long applicantId,
        String applicantName,
        Long handlerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String rejectionReason
) {}
