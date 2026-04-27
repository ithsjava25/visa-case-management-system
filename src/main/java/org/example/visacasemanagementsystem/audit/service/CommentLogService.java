package org.example.visacasemanagementsystem.audit.service;

import org.example.visacasemanagementsystem.audit.CommentEventType;
import org.example.visacasemanagementsystem.audit.dto.CommentLogDTO;
import org.example.visacasemanagementsystem.audit.entity.CommentLog;
import org.example.visacasemanagementsystem.audit.entity.CommentLog_;
import org.example.visacasemanagementsystem.audit.mapper.CommentLogMapper;
import org.example.visacasemanagementsystem.audit.repository.CommentLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentLogService {

    private final CommentLogRepository commentLogRepository;
    private final CommentLogMapper commentLogMapper;

    public CommentLogService(CommentLogRepository commentLogRepository, CommentLogMapper commentLogMapper) {
        this.commentLogRepository = commentLogRepository;
        this.commentLogMapper = commentLogMapper;

    }

    public void createCommentLog(Long actorUserId, Long visaCaseId, Long commentId, CommentEventType commentEventType, String description) {
        CommentLog commentLog = commentLogMapper.toEntity(actorUserId, visaCaseId, commentId,commentEventType, description);
        commentLogRepository.save(commentLog);

    }

    @PreAuthorize("hasRole('SYSADMIN')")
    public List<CommentLogDTO> findAll() {
        return commentLogRepository.findAll()
                .stream()
                .map(commentLogMapper::toDTO)
                .toList();
    }

    /**
     * Used by the /log/comment page. Mirror of VisaLogService#findFiltered.
     * Only non-null filters are added to the query, avoiding Hibernate 6/7's
     * inability to bind null enum-typed parameters in JPQL.
     */
    @PreAuthorize("hasRole('SYSADMIN')")
    public Page<CommentLogDTO> findFiltered(CommentEventType eventType,
                                            LocalDateTime from,
                                            LocalDateTime to,
                                            Pageable pageable) {
        // Start from the "no filter" Specification. Spring Data JPA 4 deprecated
        // the where(null) idiom in favour of this explicit factory method.
        Specification<CommentLog> spec = Specification.unrestricted();
        if (eventType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get(CommentLog_.commentEventType), eventType));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(CommentLog_.timeStamp), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(CommentLog_.timeStamp), to));
        }
        return commentLogRepository.findAll(spec, pageable).map(commentLogMapper::toDTO);
    }

}
