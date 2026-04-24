package org.example.visacasemanagementsystem.audit.mapper;

import org.example.visacasemanagementsystem.audit.FileEventType;
import org.example.visacasemanagementsystem.audit.dto.FileLogDTO;
import org.example.visacasemanagementsystem.audit.entity.FileLog;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class FileLogMapper {

    // For viewing (Entity --> DTO)
    public FileLogDTO toDTO(FileLog fileLog) {
        if (fileLog == null) return  null;

        return  new FileLogDTO(
                fileLog.getId(),
                fileLog.getTimeStamp(),
                fileLog.getActorUserId(),
                fileLog.getVisaCaseId(),
                fileLog.getFileName(),
                fileLog.getFileEventType(),
                fileLog.getDescription()
        );
    }

    // The service layer is responsible for creating the log entity
    public FileLog toEntity(Long actorUserId, Long visaCaseId, String fileName, FileEventType fileEventType, String description) {
        FileLog fileLog = new FileLog();
        fileLog.setActorUserId(actorUserId);
        fileLog.setVisaCaseId(visaCaseId);
        fileLog.setFileName(fileName);
        fileLog.setFileEventType(fileEventType);
        fileLog.setDescription(description);
        return fileLog;

    }
}
