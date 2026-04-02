package org.example.visacasemanagementsystem.visa.service;
import jakarta.persistence.EntityNotFoundException;
import org.example.visacasemanagementsystem.audit.AuditEventType;
import org.example.visacasemanagementsystem.audit.service.AuditService;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.visa.VisaStatus;
import org.example.visacasemanagementsystem.visa.VisaType;
import org.example.visacasemanagementsystem.visa.dto.CreateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.VisaDTO;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.example.visacasemanagementsystem.visa.mapper.VisaMapper;
import org.example.visacasemanagementsystem.visa.repository.VisaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class VisaService {

    private static final String NOT_FOUND_MESSAGE = "Visa not found";

    private final VisaRepository visaRepository;
    private final UserRepository userRepository;
    private final VisaMapper visaMapper;
    private final AuditService auditService;


    public VisaService(VisaRepository visaRepository, UserRepository userRepository ,VisaMapper visaMapper, AuditService auditService) {
        this.visaRepository = visaRepository;
        this.userRepository = userRepository;
        this.visaMapper = visaMapper;
        this.auditService = auditService;
    }

    // --- För filtrering i Frontend list-vy ---
    public List<Visa> findAll() {
        return visaRepository.findAll();
    }

    public Optional<Visa> findById(Long id) {
        return visaRepository.findById(id);
    }

    public List<VisaDTO> findVisaByType(VisaType visaType) {
       return visaRepository.findByVisaType(visaType, Sort.by("visaType").descending())
               .stream()
               .map(visaMapper::toDTO)
               .toList();
    }

    public List<VisaDTO> findVisaByStatus(String visaStatus) {
        VisaStatus status = VisaStatus.valueOf(visaStatus.toUpperCase());

        List<Visa> visaEntites = visaRepository.findByVisaStatus(
                status,
                Sort.by("visaType").descending()
        );

        return visaEntites.stream()
                .map(visaMapper::toDTO)
                .toList();
    }

    public List<VisaDTO> findVisaByDateCreated(LocalDate date){
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return visaRepository.findByCreatedAtBetween(
                startOfDay,
                endOfDay,
                Sort.by("createdAt").descending())
                .stream()
                .map(visaMapper::toDTO)
                .toList();
    }

    public List<VisaDTO> findVisaByDateUpdated(LocalDateTime since){
        return visaRepository.findByUpdatedAtAfter(since, Sort.by("updatedAt").descending())
                .stream()
                .map(visaMapper::toDTO)
                .toList();
    }

    // ---

    @Transactional
    public VisaDTO updateVisaStatus(Long visaId, VisaStatus newStatus, Long userId) {
        // Hämta visa
        Visa visa = visaRepository.findById(visaId)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND_MESSAGE));

        // Addera logik för att kontrollera korrekt behörighet?

        // Uppdatera statusen
        visa.setVisaStatus(newStatus);
        Visa savedVisa = visaRepository.save(visa);

        // Logga händelsen
        auditService.createAuditLog(
                userId,
                visaId,
                AuditEventType.UPDATED,
                "Status changed to: " + newStatus
        );

        // Returnera DTO
        return visaMapper.toDTO(savedVisa);
    }

    // Skapa ansökan
    @Transactional
    public VisaDTO applyForVisa(CreateVisaDTO dto, Long userId, String reason) {
        // Mappar grunddata
        Visa visa = visaMapper.toEntity(dto);

        // Hämtar användaren
        User applicant = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Sätter initial status och kopplar användare
        visa.setApplicant(applicant);
        visa.setVisaStatus(VisaStatus.SUBMITTED);

        Visa savedVisa = visaRepository.save(visa);

        // Logga händelsen
        auditService.createAuditLog(
                userId,
                savedVisa.getId(),
                org.example.visacasemanagementsystem.audit.AuditEventType.CREATED,
                "Visa application submitted."
        );
        return visaMapper.toDTO(savedVisa);
    }

    // Godkänn ansökan
    @Transactional
    public VisaDTO approveVisa(Long visaId, Long adminId) {
        Visa visa = visaRepository.findById(visaId) // Addera logik och anropa metod här i service istället för repo??
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND_MESSAGE));

        // Addera logik för att kontrollera korrekt behörighet?

        visa.setVisaStatus(VisaStatus.GRANTED);
        Visa savedVisa = visaRepository.save(visa);

        // Logga händelsen
        auditService.createAuditLog(
                adminId,
                visaId,
                org.example.visacasemanagementsystem.audit.AuditEventType.GRANTED,
                "Visa has been granted."
        );

        return visaMapper.toDTO(savedVisa);
    }

    // Neka ansökan
    @Transactional
    public VisaDTO rejectVisa(Long visaId, Long adminId, String reason) {
        Visa visa = visaRepository.findById(visaId)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND_MESSAGE));

        // Addera logik för att kontrollera korrekt behörighet?

        visa.setVisaStatus(VisaStatus.REJECTED);
        visa.setRejectionReason(reason);

        Visa savedVisa = visaRepository.save(visa);

        auditService.createAuditLog(adminId, visaId, org.example.visacasemanagementsystem.audit.AuditEventType.REJECTED, "Visa rejected. Reason: " + reason);

        return visaMapper.toDTO(savedVisa);
    }

    // Assign Handler


    // Hjälpmetoder

    // ValidateHandler --> Bekräfta att en handläggare har rätt behörighet
    public User validateHandler(Long handlerId) throws UnauthorizedException {
        // Hämta användare
        User  user = userRepository.findById(handlerId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Kontrollera behörighetet
        boolean isAdmin = user.getUserAuthorization() == UserAuthorization.ADMIN;
        boolean isSysAdmin = user.getUserAuthorization() == UserAuthorization.SYSADMIN;

        if (!isAdmin && !isSysAdmin) {
            throw  new UnauthorizedException("User is not authorized to perform this action");
        }
        return user;
    }

}
