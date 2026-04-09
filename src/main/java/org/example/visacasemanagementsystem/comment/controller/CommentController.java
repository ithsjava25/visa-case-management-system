package org.example.visacasemanagementsystem.comment.controller;

import jakarta.validation.Valid;
import org.example.visacasemanagementsystem.comment.dto.CommentDTO;
import org.example.visacasemanagementsystem.comment.dto.CreateCommentDTO;
import org.example.visacasemanagementsystem.comment.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // Create new comment
    @PostMapping
    public ResponseEntity<CommentDTO> createComment(@Valid @RequestBody CreateCommentDTO dto){
        CommentDTO createComment =  commentService.createComment(dto);
        return new ResponseEntity<>(createComment, HttpStatus.CREATED);
    }

    // Get all comments for a selected visa case
    @GetMapping("/visa/{visaId}")
    public ResponseEntity<List<CommentDTO>> getCommentsByVisa(@PathVariable Long visaId){
        List<CommentDTO> comments = commentService.getCommentsByVisaId(visaId);
        return new ResponseEntity<>(comments, HttpStatus.OK);

    }
}
