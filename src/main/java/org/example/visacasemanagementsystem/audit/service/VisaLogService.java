package org.example.visacasemanagementsystem.audit.service;

import org.example.visacasemanagementsystem.audit.VisaEventType;
import org.example.visacasemanagementsystem.audit.dto.VisaLogDTO;
import org.example.visacasemanagementsystem.audit.entity.VisaLog;
import org.example.visacasemanagementsystem.audit.entity.VisaLog_;
import org.example.visacasemanagementsystem.audit.mapper.VisaLogMapper;
import org.example.visacasemanagementsystem.audit.repository.VisaLogRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    @PreAuthorize("isAuthenticated()")
    public List<VisaLogDTO> findAll() {
        return visaLogRepository.findAll()
                .stream()
                .map(visaLogMapper::toDTO)
                .toList();
    }

    /**
     * Used by the /log/visa page. Any of the three filter values can be null —
     * only non-null filters are added to the query, so Hibernate never has to
     * bind a null enum parameter (which fails in Hibernate 6/7).
     */
    public Page<VisaLogDTO> findFiltered(VisaEventType eventType,
                                         LocalDateTime from,
                                         LocalDateTime to,
                                         Pageable pageable) {
        // Start from the "no filter" Specification. Spring Data JPA 4 deprecated
        // the where(null) idiom in favour of this explicit factory method.
        Specification<VisaLog> spec = Specification.unrestricted();
        if (eventType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get(VisaLog_.visaEventType), eventType));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(VisaLog_.timeStamp), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(VisaLog_.timeStamp), to));
        }
        return visaLogRepository.findAll(spec, pageable).map(visaLogMapper::toDTO);
    }
}
