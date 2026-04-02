package org.example.visacasemanagementsystem.audit.service;

import org.example.visacasemanagementsystem.audit.AuditEventType;
import org.example.visacasemanagementsystem.audit.entity.AuditLog;
import org.example.visacasemanagementsystem.audit.mapper.AuditMapper;
import org.example.visacasemanagementsystem.audit.repository.AuditRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditRepository auditRepository;
    private final AuditMapper auditMapper;

    public AuditService(AuditRepository auditRepository, AuditMapper auditMapper) {
        this.auditRepository = auditRepository;
        this.auditMapper = auditMapper;
    }

    public void createLog(Long userId, Long visaCaseId, AuditEventType auditEventType, String description) {
        AuditLog auditLog = auditMapper.toEntity(userId, visaCaseId, auditEventType, description);
        auditRepository.save(auditLog);
    }

}
