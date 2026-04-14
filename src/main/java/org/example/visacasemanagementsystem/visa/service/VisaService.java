package org.example.visacasemanagementsystem.visa.service;
import jakarta.persistence.EntityNotFoundException;
import org.example.visacasemanagementsystem.audit.AuditEventType;
import org.example.visacasemanagementsystem.audit.service.AuditService;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.visa.VisaStatus;
import org.example.visacasemanagementsystem.visa.VisaType;
import org.example.visacasemanagementsystem.visa.dto.CreateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.UpdateVisaDTO;
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
import java.util.Locale;

@Service
public class VisaService {

    private static final String NOT_FOUND_MESSAGE = "Visa not found.";

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

    // --- For filtering in Frontend list-view ---
    public List<VisaDTO> findAll() {
        return visaRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream()
                .map(visaMapper::toDTO)
                .toList();
    }

    private Visa findVisaById(Long id) {
       return visaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND_MESSAGE));
    }

    public VisaDTO findVisaDtoById(Long id) {
        Visa visa = visaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND_MESSAGE));

        return visaMapper.toDTO(visa);
    }

    public List<VisaDTO> findVisasByApplicant(Long applicantId) {
        return visaRepository.findByApplicant_Id(applicantId, Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream()
                .map(visaMapper::toDTO)
                .toList();
    }

    public List<VisaDTO> findVisaByType(VisaType visaType) {
       return visaRepository.findByVisaType(visaType, Sort.by("visaType").descending())
               .stream()
               .map(visaMapper::toDTO)
               .toList();
    }

    public List<VisaDTO> findVisaByStatus(String visaStatus) {
        if(visaStatus == null || visaStatus.isBlank()) {
            throw new IllegalArgumentException("Visa status cannot be null or blank");
        }

        VisaStatus status;
        try {
            status = VisaStatus.valueOf(visaStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Invalid visa status: " + visaStatus);
        }

        List<Visa> visaEntites = visaRepository.findByVisaStatus(
                status,
                Sort.by("visaType").descending()
        );

        return visaEntites.stream()
                .map(visaMapper::toDTO)
                .toList();
    }

    public List<VisaDTO> findVisaByDateCreated(LocalDate date){
        if (date == null) {
            throw new IllegalArgumentException("Visa date cannot be null");
        }

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
        if (since == null) {
            throw new IllegalArgumentException("Updated-since timestamp cannot be null");
        }

        return visaRepository.findByUpdatedAtAfter(since, Sort.by("updatedAt").descending())
                .stream()
                .map(visaMapper::toDTO)
                .toList();
    }

    // -----

    // Todo: Ta bort denna metod? --> då visa status sätts genom andra metoder/knappar
    @Transactional
    public VisaDTO updateVisaStatus(Long visaId, VisaStatus newStatus, Long adminId) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New visa status cannot be null");
        }

        if (newStatus == VisaStatus.REJECTED) {
            throw new IllegalArgumentException("Use rejectVisa(...) when rejecting a visa.");
        }

        validateHandler(adminId);
        Visa visa = findVisaById(visaId);

        // Update visa status
        visa.setVisaStatus(newStatus);
        visa.setStatusInformation(null);
        Visa savedVisa = visaRepository.save(visa);

        // Create log in database
        auditService.createAuditLog(
                adminId,
                visaId,
                AuditEventType.UPDATED,
                "Status changed to: " + newStatus
        );

        // return dto
        return visaMapper.toDTO(savedVisa);
    }

    @Transactional
    public VisaDTO applyForVisa(CreateVisaDTO dto, Long userId) {
        // Validate travel date
        validateTravelDate(dto.travelDate());

        // Maps data
        Visa visa = visaMapper.toEntity(dto);

        // Get user
        User applicant = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        visa.setApplicant(applicant);
        visa.setVisaStatus(VisaStatus.SUBMITTED);
        Visa savedVisa = visaRepository.save(visa);

        // Create log in database
        auditService.createAuditLog(
                userId,
                savedVisa.getId(),
                AuditEventType.CREATED,
                "Visa application submitted."
        );
        return visaMapper.toDTO(savedVisa);
    }

    @Transactional
    public VisaDTO updateVisa(Long visaId, UpdateVisaDTO dto, Long userId) {
        if (!visaId.equals(dto.id())) {
            throw new IllegalArgumentException("Mismatched visa id.");
        }

        Visa visa = visaRepository.findById(visaId)
                .orElseThrow(() -> new EntityNotFoundException("Visa not found"));

        if (!visa.getApplicant().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this application.");
        }

        if (visa.getVisaStatus() != VisaStatus.INCOMPLETE && visa.getVisaStatus() != VisaStatus.SUBMITTED ) {
            throw new IllegalArgumentException("This application can no longer be edited.");
        }

       validateTravelDate(dto.travelDate());

        visa.setVisaType(dto.visaType());
        visa.setNationality(dto.nationality());
        visa.setPassportNumber(dto.passportNumber());
        visa.setTravelDate(dto.travelDate());

        visa.setVisaStatus(VisaStatus.SUBMITTED);
        visa.setStatusInformation(null);

        Visa  savedVisa = visaRepository.save(visa);

        auditService.createAuditLog(
                userId,
                savedVisa.getId(),
                AuditEventType.UPDATED,
                "Applicant updated application details."
        );
        return visaMapper.toDTO(savedVisa);
    }

    @Transactional
    public VisaDTO approveVisa(Long visaId, Long adminId) {
        User admin = validateHandler(adminId);
        Visa visa = findVisaById(visaId);

        if (visa.getHandler() == null) {
            visa.setHandler(admin);
        }

        visa.setVisaStatus(VisaStatus.GRANTED);
        visa.setStatusInformation(null);
        Visa savedVisa = visaRepository.save(visa);

        // Create log in database
        auditService.createAuditLog(
                adminId,
                visaId,
                AuditEventType.GRANTED,
                "Visa has been granted."
        );

        return visaMapper.toDTO(savedVisa);
    }

    @Transactional
    public VisaDTO rejectVisa(Long visaId, Long adminId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason for rejection cannot be null or blank");
        }

        User admin = validateHandler(adminId);
        Visa visa = findVisaById(visaId);

        if (visa.getHandler() == null) {
            visa.setHandler(admin);
        }

        visa.setVisaStatus(VisaStatus.REJECTED);
        visa.setStatusInformation(reason);
        Visa savedVisa = visaRepository.save(visa);

        // Create log in database
        auditService.createAuditLog(
                adminId,
                visaId,
                AuditEventType.REJECTED,
                "Visa rejected. Reason: " + reason);

        return visaMapper.toDTO(savedVisa);
    }

    @Transactional
    public VisaDTO requestMoreInformation(Long visaId, Long adminId, String infoText) {
        if (infoText == null || infoText.isBlank()) {
            throw  new IllegalArgumentException("Information request text cannot be null or blank");
        }

        User  admin = validateHandler(adminId);
        Visa visa = findVisaById(visaId);

        if (visa.getHandler() == null) {
            visa.setHandler(admin);
        }

        visa.setVisaStatus(VisaStatus.INCOMPLETE);
        visa.setStatusInformation(infoText);
        Visa savedVisa = visaRepository.save(visa);

        // Create log in database
        auditService.createAuditLog(
                adminId,
                visaId,
                AuditEventType.UPDATED,
                "Information requested: " + infoText
        );

        return visaMapper.toDTO(savedVisa);
    }

    @Transactional
    public VisaDTO assignHandler(Long visaId, Long adminId) {
        User admin = validateHandler(adminId);
        Visa visa = findVisaById(visaId);

        visa.setHandler(admin); // Connects handler
        visa.setVisaStatus(VisaStatus.ASSIGNED);

        Visa savedVisa = visaRepository.save(visa);

        // Create log in database
        auditService.createAuditLog(
                adminId,
                visaId,
                AuditEventType.ASSIGNED,
                "Admin " + admin.getFullName() + " has been assigned to case and status is now ASSIGNED."
        );
        return visaMapper.toDTO(savedVisa);
    }

    // ---- HELP METHODS ----

    public User validateHandler(Long handlerId) throws UnauthorizedException {
        // Get user by id
        User user = userRepository.findById(handlerId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        // Checks Authorization
        boolean isAdmin = user.getUserAuthorization() == UserAuthorization.ADMIN;
        boolean isSysAdmin = user.getUserAuthorization() == UserAuthorization.SYSADMIN;

        if (!isAdmin && !isSysAdmin) {
            throw new UnauthorizedException("User is not authorized to perform this action.");
        }
        return user;
    }

    public void validateTravelDate(LocalDate travelDate) {
        if (travelDate == null) {
            throw new IllegalArgumentException("Travel date cannot be in the past.");
        }

        if (travelDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Travel date cannot be in the past.");
        }
    }


    public List<VisaDTO> findVisasByApplicantId(Long applicantId) {
        return visaRepository.findVisasByApplicantId(applicantId,
                        Sort.by("updatedAt").descending())
                .stream()
                .map(visaMapper::toDTO)
                .toList();
    }

    public List<VisaDTO> findVisasByHandlerId(Long handlerId) {
        return visaRepository.findVisasByHandlerId(handlerId,
                        Sort.by("updatedAt").descending())
                .stream()
                .map(visaMapper::toDTO)
                .toList();
    }
}
