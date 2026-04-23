package org.example.visacasemanagementsystem.audit.service;

import org.example.visacasemanagementsystem.audit.UserEventType;
import org.example.visacasemanagementsystem.audit.dto.UserLogDTO;
import org.example.visacasemanagementsystem.audit.entity.UserLog;
import org.example.visacasemanagementsystem.audit.mapper.UserLogMapper;
import org.example.visacasemanagementsystem.audit.repository.UserLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserLogService {

    private final UserLogRepository userLogRepository;
    private final UserLogMapper userLogMapper;

    public UserLogService(UserLogRepository userLogRepository, UserLogMapper userLogMapper) {
        this.userLogRepository = userLogRepository;
        this.userLogMapper = userLogMapper;
    }

    public void createUserLog(Long actorUserId, Long targetUserId, UserEventType userEventType, String description) {
        UserLog userLog = userLogMapper.toEntity(actorUserId, targetUserId, userEventType, description);
        userLogRepository.save(userLog);
    }

    public List<UserLogDTO> findAll() {
        return userLogRepository.findAll()
                .stream()
                .map(userLogMapper::toDTO)
                .toList();
    }
}
