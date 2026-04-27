package org.example.visacasemanagementsystem.audit.repository;

import org.example.visacasemanagementsystem.audit.entity.VisaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VisaLogRepository extends JpaRepository<VisaLog, Long>, JpaSpecificationExecutor<VisaLog> {
}
