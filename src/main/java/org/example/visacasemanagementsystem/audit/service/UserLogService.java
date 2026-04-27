package org.example.visacasemanagementsystem.audit.service;

import org.example.visacasemanagementsystem.audit.UserEventType;
import org.example.visacasemanagementsystem.audit.dto.UserLogDTO;
import org.example.visacasemanagementsystem.audit.entity.UserLog;
import org.example.visacasemanagementsystem.audit.entity.UserLog_;
import org.example.visacasemanagementsystem.audit.mapper.UserLogMapper;
import org.example.visacasemanagementsystem.audit.repository.UserLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    @PreAuthorize("isAuthenticated()")
    public List<UserLogDTO> findAll() {
        return userLogRepository.findAll()
                .stream()
                .map(userLogMapper::toDTO)
                .toList();
    }

    /**
     * Used by the /log/user page. Mirror of VisaLogService#findFiltered.
     * Only non-null filters are added to the query, avoiding Hibernate 6/7's
     * inability to bind null enum-typed parameters in JPQL.
     */
    @PreAuthorize("hasRole('SYSADMIN')")
    public Page<UserLogDTO> findFiltered(UserEventType eventType,
                                         LocalDateTime from,
                                         LocalDateTime to,
                                         Pageable pageable) {
        // Start from the "no filter" Specification. Spring Data JPA 4 deprecated
        // the where(null) idiom in favour of this explicit factory method.
        Specification<UserLog> spec = Specification.unrestricted();
        if (eventType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get(UserLog_.userEventType), eventType));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(UserLog_.timeStamp), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(UserLog_.timeStamp), to));
        }
        return userLogRepository.findAll(spec, pageable).map(userLogMapper::toDTO);
    }
}
