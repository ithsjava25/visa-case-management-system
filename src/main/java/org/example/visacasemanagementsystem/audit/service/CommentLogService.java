package org.example.visacasemanagementsystem.audit.service;

import org.example.visacasemanagementsystem.audit.CommentEventType;
import org.example.visacasemanagementsystem.audit.dto.CommentLogDTO;
import org.example.visacasemanagementsystem.audit.entity.CommentLog;
import org.example.visacasemanagementsystem.audit.mapper.CommentLogMapper;
import org.example.visacasemanagementsystem.audit.repository.CommentLogRepository;
import org.springframework.stereotype.Service;

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

    public List<CommentLogDTO> findAll() {
        return commentLogRepository.findAll()
                .stream()
                .map(commentLogMapper::toDTO)
                .toList();
    }

}
