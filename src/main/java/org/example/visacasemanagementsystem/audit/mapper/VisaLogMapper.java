package org.example.visacasemanagementsystem.audit.mapper;

import org.example.visacasemanagementsystem.audit.VisaEventType;
import org.example.visacasemanagementsystem.audit.dto.VisaLogDTO;
import org.example.visacasemanagementsystem.audit.entity.VisaLog;
import org.springframework.stereotype.Component;


@Component
public class VisaLogMapper {

    // For viewing (Entity --> DTO)
    public VisaLogDTO toDTO(VisaLog visaLog) {
        if (visaLog == null) return null;

        return new VisaLogDTO(
                visaLog.getId(),
                visaLog.getTimeStamp(),
                visaLog.getUserId(),
                visaLog.getVisaCaseId(),
                visaLog.getVisaEventType(),
                visaLog.getDescription()
        );
    }


    // The service layer is responsible for creating the log entity
    public VisaLog toEntity(Long userId, Long visaCaseId, VisaEventType visaEventType, String description) {
        VisaLog visaLog = new VisaLog();
        visaLog.setUserId(userId);
        visaLog.setVisaCaseId(visaCaseId);
        visaLog.setVisaEventType(visaEventType);
        visaLog.setDescription(description);
        return visaLog;
    }
}
