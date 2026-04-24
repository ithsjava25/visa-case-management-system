package org.example.visacasemanagementsystem.audit.repository;

import org.example.visacasemanagementsystem.audit.entity.FileLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileLogRepository  extends JpaRepository<FileLog, Long> {
}
