package org.example.visacasemanagementsystem.log.mapper;

import org.example.visacasemanagementsystem.log.LogEvent;
import org.example.visacasemanagementsystem.log.dto.LogDTO;
import org.example.visacasemanagementsystem.log.entity.Log;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LogMapper {

    // För visning (Entity --> DTO)
    public LogDTO toDTO(Log log){
        if (log == null) return null;

        return  new LogDTO(
                log.getId(),
                log.getTimeStamp(),
                log.getUserId(),
                log.getVisaCaseId(),
                log.getLogEvent(),
                log.getDescription()
        );
    }


    // Service-klassen kommer senare skapa denna log
    public Log toEntity(Long userId, Long visaCaseId, LogEvent logEvent, String description){
        Log log = new Log();
        log.setUserId(userId);
        log.setVisaCaseId(visaCaseId);
        log.setLogEvent(logEvent);
        log.setDescription(description);
        log.setTimeStamp(LocalDateTime.now());
        return log;

    }
}
