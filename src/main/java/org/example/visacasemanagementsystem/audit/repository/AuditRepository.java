package org.example.visacasemanagementsystem.audit.repository;

import org.example.visacasemanagementsystem.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends JpaRepository<AuditLog, Long> {
}
