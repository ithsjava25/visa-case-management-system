package org.example.visacasemanagementsystem.audit.service;

import org.example.visacasemanagementsystem.audit.VisaEventType;
import org.example.visacasemanagementsystem.audit.dto.VisaLogDTO;
import org.example.visacasemanagementsystem.audit.entity.VisaLog;
import org.example.visacasemanagementsystem.audit.mapper.VisaLogMapper;
import org.example.visacasemanagementsystem.audit.repository.VisaLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VisaLogService {

    private final VisaLogRepository visaLogRepository;
    private final VisaLogMapper visaLogMapper;

    public VisaLogService(VisaLogRepository visaLogRepository, VisaLogMapper visaLogMapper) {
        this.visaLogRepository = visaLogRepository;
        this.visaLogMapper = visaLogMapper;
    }

    public void createVisaLog(Long userId, Long visaCaseId, VisaEventType visaEventType, String description) {
        VisaLog visaLog = visaLogMapper.toEntity(userId, visaCaseId, visaEventType, description);
        visaLogRepository.save(visaLog);
    }

    public List<VisaLogDTO> findAll() {
        return visaLogRepository.findAll()
                .stream()
                .map(visaLogMapper::toDTO)
                .toList();
    }
}
