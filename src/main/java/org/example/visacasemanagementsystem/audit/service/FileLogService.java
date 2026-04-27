package org.example.visacasemanagementsystem.audit.service;

import org.example.visacasemanagementsystem.audit.FileEventType;
import org.example.visacasemanagementsystem.audit.dto.FileLogDTO;
import org.example.visacasemanagementsystem.audit.entity.FileLog;
import org.example.visacasemanagementsystem.audit.mapper.FileLogMapper;
import org.example.visacasemanagementsystem.audit.repository.FileLogRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

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
}
