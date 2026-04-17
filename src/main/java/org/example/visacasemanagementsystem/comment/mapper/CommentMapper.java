package org.example.visacasemanagementsystem.comment.mapper;

import org.example.visacasemanagementsystem.comment.dto.CommentDTO;
import org.example.visacasemanagementsystem.comment.dto.CreateCommentDTO;
import org.example.visacasemanagementsystem.comment.entity.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    // För visning (Entity --> DTO)
    public CommentDTO toDTO(Comment comment) {
        if  (comment == null) return null;

        return new CommentDTO(
                comment.getId(),
                comment.getVisa() != null ? comment.getVisa().getId() : null,
                comment.getAuthor() != null ? comment.getAuthor().getFullName() : "System", // autofallback
                comment.getText(),
                comment.getCreatedAt()
        );
    }

    public Comment toEntity(CreateCommentDTO dto) {
        if  (dto == null) return null;

        Comment comment = new Comment();
        comment.setText(dto.text());
        return  comment;
    }
}
