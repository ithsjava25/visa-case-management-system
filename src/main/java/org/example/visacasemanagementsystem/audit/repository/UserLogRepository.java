package org.example.visacasemanagementsystem.audit.repository;

import org.example.visacasemanagementsystem.audit.entity.UserLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLogRepository extends JpaRepository<UserLog, Long>, JpaSpecificationExecutor<UserLog> {
}
