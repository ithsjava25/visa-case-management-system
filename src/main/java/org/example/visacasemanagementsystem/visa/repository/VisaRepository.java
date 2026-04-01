package org.example.visacasemanagementsystem.visa.repository;

import org.example.visacasemanagementsystem.visa.VisaStatus;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisaRepository extends JpaRepository<Visa,Long> {

    // Todo: Skapa enum för tillgängliga visatyper istället för att te emot String?
    List<Visa> findByVisaTypeContainingIgnoreCase(String visaType, Sort type);

    List<Visa> findByVisaStatus(VisaStatus visaStatus, Sort sort);

    List<Visa> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Sort sort);

    List<Visa> findByUpdatedAtAfter(LocalDateTime lastLogin, Sort sort);



}
