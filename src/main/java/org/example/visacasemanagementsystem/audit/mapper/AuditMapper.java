package org.example.visacasemanagementsystem.audit.mapper;

import org.example.visacasemanagementsystem.audit.AuditEventType;
import org.example.visacasemanagementsystem.audit.dto.AuditDTO;
import org.example.visacasemanagementsystem.audit.entity.AuditLog;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditMapper {

    // För visning (Entity --> DTO)
    public AuditDTO toDTO(AuditLog auditLog){
        if (auditLog == null) return null;

        return  new AuditDTO(
                auditLog.getId(),
                auditLog.getTimeStamp(),
                auditLog.getUserId(),
                auditLog.getVisaCaseId(),
                auditLog.getAuditEventType(),
                auditLog.getDescription()
        );
    }


    // Service-klassen sköter skapandet av denna log
    public AuditLog toEntity(Long userId, Long visaCaseId, AuditEventType auditEventType, String description){
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setVisaCaseId(visaCaseId);
        auditLog.setAuditEventType(auditEventType);
        auditLog.setDescription(description);
        auditLog.setTimeStamp(LocalDateTime.now());
        return auditLog;

    }
}
