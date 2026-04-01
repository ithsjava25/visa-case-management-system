package org.example.visacasemanagementsystem.log.service;

import org.example.visacasemanagementsystem.log.LogEvent;
import org.example.visacasemanagementsystem.log.entity.Log;
import org.example.visacasemanagementsystem.log.mapper.LogMapper;
import org.example.visacasemanagementsystem.log.repository.LogRepository;
import org.springframework.stereotype.Service;

@Service
public class LogService {

    private final LogRepository logRepository;
    private final LogMapper logMapper;

    public LogService(LogRepository logRepository, LogMapper logMapper) {
        this.logRepository = logRepository;
        this.logMapper = logMapper;
    }

    public void createLog(Long userId, Long visaCaseId, LogEvent logEvent, String description) {
        Log log  = logMapper.toEntity(userId, visaCaseId, logEvent, description);
        logRepository.save(log);
    }

}
