package org.example.visacasemanagementsystem.comment.repository;

import org.example.visacasemanagementsystem.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByVisaIdOrderByCreatedAtDesc(Long visaId);
}
