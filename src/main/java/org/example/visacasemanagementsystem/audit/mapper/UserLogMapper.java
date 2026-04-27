package org.example.visacasemanagementsystem.audit.mapper;

import org.example.visacasemanagementsystem.audit.UserEventType;
import org.example.visacasemanagementsystem.audit.dto.UserLogDTO;
import org.example.visacasemanagementsystem.audit.entity.UserLog;
import org.springframework.stereotype.Component;


@Component
public class UserLogMapper {

    // For viewing (Entity --> DTO)
    public UserLogDTO toDTO(UserLog userLog) {
        if (userLog == null) return null;

        return new UserLogDTO(
                userLog.getId(),
                userLog.getTimeStamp(),
                userLog.getActorUserId(),
                userLog.getTargetUserId(),
                userLog.getUserEventType(),
                userLog.getDescription()
        );
    }


    // The service layer is responsible for creating the log entity
    public UserLog toEntity(Long actorUserId, Long targetUserId, UserEventType userEventType, String description) {
        UserLog userLog = new UserLog();
        userLog.setActorUserId(actorUserId);
        userLog.setTargetUserId(targetUserId);
        userLog.setUserEventType(userEventType);
        userLog.setDescription(description);
        return userLog;
    }
}
