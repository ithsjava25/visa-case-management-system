package org.example.visacasemanagementsystem.audit.repository;

import org.example.visacasemanagementsystem.audit.entity.CommentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLogRepository extends JpaRepository<CommentLog, Long> {
}
