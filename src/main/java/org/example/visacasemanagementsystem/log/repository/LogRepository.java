package org.example.visacasemanagementsystem.log.repository;

import org.example.visacasemanagementsystem.log.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {
}
