package org.example.visacasemanagementsystem.comment.service;

import org.example.visacasemanagementsystem.audit.VisaEventType;
import org.example.visacasemanagementsystem.audit.service.VisaLogService;
import org.example.visacasemanagementsystem.comment.dto.CommentDTO;
import org.example.visacasemanagementsystem.comment.dto.CreateCommentDTO;
import org.example.visacasemanagementsystem.comment.entity.Comment;
import org.example.visacasemanagementsystem.comment.mapper.CommentMapper;
import org.example.visacasemanagementsystem.comment.repository.CommentRepository;
import org.example.visacasemanagementsystem.exception.ResourceNotFoundException;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.example.visacasemanagementsystem.visa.repository.VisaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UserRepository userRepository;
    private final VisaRepository visaRepository;
    private final VisaLogService visaLogService;

    public CommentService(CommentRepository commentRepository, CommentMapper commentMapper, UserRepository userRepository, VisaRepository visaRepository, VisaLogService visaLogService) {
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
        this.userRepository = userRepository;
        this.visaRepository = visaRepository;
        this.visaLogService = visaLogService;
    }


     // Create Comment
    @Transactional
    public CommentDTO createComment(CreateCommentDTO dto, Long userId) {
        if (dto == null) {
            throw new IllegalArgumentException("Comment payload cannot be null");
        }

        if (dto.text() == null || dto.text().isBlank()) {
            throw new IllegalArgumentException("Comment text cannot be empty");
        }

        // Get User and Visa from database
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Visa visa = visaRepository.findById(dto.visaId())
                .orElseThrow(() -> new ResourceNotFoundException("Visa case not found with id: " + dto.visaId()));

        // Map DTO to Entity
        Comment comment = commentMapper.toEntity(dto);

        // Set author and visaCase
        comment.setAuthor(author);
        comment.setVisa(visa);

        // Save and return
        Comment savedComment = commentRepository.save(comment);

        // Create log in database
        visaLogService.createVisaLog(
                author.getId(),
                visa.getId(),
                VisaEventType.UPDATED,
                "Comment added by " + author.getFullName()
        );

        return commentMapper.toDTO(savedComment);
    }

    // Get all comments for a selected visa case
    @Transactional (readOnly = true)
    public List<CommentDTO> getCommentsByVisaId(Long visaId) {
        if (visaId == null || visaId <= 0) {
            throw new IllegalArgumentException("Visa ID must be a positive number");
        }

       List<Comment> comments = commentRepository.findByVisaIdOrderByCreatedAtDesc(visaId);
       if(comments.isEmpty() && !visaRepository.existsById(visaId)) {
           throw new ResourceNotFoundException("Visa case not found with id: " + visaId);
       }

       return comments.stream()
               .map(commentMapper::toDTO)
               .toList();
    }
}