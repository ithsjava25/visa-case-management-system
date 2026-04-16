package org.example.visacasemanagementsystem.visa.mapper;

import org.example.visacasemanagementsystem.visa.dto.CreateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.UpdateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.VisaDTO;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.springframework.stereotype.Component;

@Component
public class VisaMapper {

    // För visning (Entity --> DTO)
    public VisaDTO toDTO(Visa visa) {
        if  (visa == null) return null;

        return new VisaDTO(
                visa.getId(),
                visa.getVisaType(),
                visa.getVisaStatus(),
                visa.getNationality(),
                visa.getPassportNumber(),
                visa.getTravelDate(),
                visa.getApplicant() != null ? visa.getApplicant().getId() : null,
                visa.getApplicant() != null ? visa.getApplicant().getFullName() : "Unknown",
                visa.getHandler() != null ? visa.getHandler().getId() : null,
                visa.getHandler() != null ? visa.getHandler().getFullName() : "Unassigned",
                visa.getCreatedAt(),
                visa.getUpdatedAt(),
                visa.getStatusInformation()

        );
    }

    // CreateDTO --> Entity
    public Visa toEntity(CreateVisaDTO dto) {
        if (dto == null) return null;

        Visa visa = new Visa();
        visa.setVisaType(dto.visaType());
        visa.setNationality(dto.nationality());
        visa.setPassportNumber(dto.passportNumber());
        visa.setTravelDate(dto.travelDate());
        return visa;
    }

    // Uppdatering (DTO --> Befintlig Entity)
    public void updateEntityFromDTO(UpdateVisaDTO dto, Visa visa) {
        if (dto == null || visa == null) return;

        visa.setVisaType(dto.visaType());
        visa.setVisaStatus(dto.visaStatus());
        visa.setNationality(dto.nationality());
        visa.setPassportNumber(dto.passportNumber());
        visa.setTravelDate(dto.travelDate());

    }


}
