package org.example.visacasemanagementsystem.audit.mapper;

import org.example.visacasemanagementsystem.audit.CommentEventType;
import org.example.visacasemanagementsystem.audit.dto.CommentLogDTO;
import org.example.visacasemanagementsystem.audit.entity.CommentLog;
import org.springframework.stereotype.Component;

@Component
public class CommentLogMapper {

    // For viewing (Entity --> DTO)
    public CommentLogDTO toDTO(CommentLog commentLog) {
        if (commentLog == null) return  null;

        return new CommentLogDTO(
                commentLog.getId(),
                commentLog.getTimeStamp(),
                commentLog.getActorUserId(),
                commentLog.getVisaCaseId(),
                commentLog.getCommentId(),
                commentLog.getCommentEventType(),
                commentLog.getDescription()
        );
    }

    // The service layer is responsible for creating the log entity
    public CommentLog toEntity(Long actorUserId, Long visaCaseId, Long commentId, CommentEventType commentEventType, String description) {
        CommentLog commentLog = new CommentLog();
        commentLog.setActorUserId(actorUserId);
        commentLog.setVisaCaseId(visaCaseId);
        commentLog.setCommentId(commentId);
        commentLog.setCommentEventType(commentEventType);
        commentLog.setDescription(description);
        return commentLog;
    }
}
