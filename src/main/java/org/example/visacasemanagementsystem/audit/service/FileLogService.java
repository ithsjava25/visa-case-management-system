package org.example.visacasemanagementsystem.audit.service;

import org.example.visacasemanagementsystem.audit.FileEventType;
import org.example.visacasemanagementsystem.audit.dto.FileLogDTO;
import org.example.visacasemanagementsystem.audit.entity.FileLog;
import org.example.visacasemanagementsystem.audit.entity.FileLog_;
import org.example.visacasemanagementsystem.audit.mapper.FileLogMapper;
import org.example.visacasemanagementsystem.audit.repository.FileLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileLogService {

    private final FileLogRepository fileLogRepository;
    private final FileLogMapper fileLogMapper;

    public FileLogService(FileLogRepository fileLogRepository, FileLogMapper fileLogMapper) {
        this.fileLogRepository = fileLogRepository;
        this.fileLogMapper = fileLogMapper;
    }

    public void createFileLog(Long actorUserId, Long visaCaseId, String fileName, FileEventType fileEventType, String description) {
        FileLog fileLog = fileLogMapper.toEntity(actorUserId, visaCaseId, fileName, fileEventType, description);
        fileLogRepository.save(fileLog);

    }

    @PreAuthorize("isAuthenticated()")
    public List<FileLogDTO> findAll() {
        return fileLogRepository.findAll()
                .stream()
                .map(fileLogMapper::toDTO)
                .toList();
    }

    /**
     * Used by the /log/file page. Mirror of VisaLogService#findFiltered.
     * Only non-null filters are added to the query, avoiding Hibernate 6/7's
     * inability to bind null enum-typed parameters in JPQL.
     */
    @PreAuthorize("hasRole('SYSADMIN')")
    public Page<FileLogDTO> findFiltered(FileEventType eventType,
                                         LocalDateTime from,
                                         LocalDateTime to,
                                         Pageable pageable) {
        // Start from the "no filter" Specification. Spring Data JPA 4 deprecated
        // the where(null) idiom in favour of this explicit factory method.
        Specification<FileLog> spec = Specification.unrestricted();
        if (eventType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get(FileLog_.fileEventType), eventType));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(FileLog_.timeStamp), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(FileLog_.timeStamp), to));
        }
        return fileLogRepository.findAll(spec, pageable).map(fileLogMapper::toDTO);
    }
}
