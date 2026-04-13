package org.example.visacasemanagementsystem.visa;

import jakarta.persistence.EntityNotFoundException;
import org.example.visacasemanagementsystem.audit.AuditEventType;
import org.example.visacasemanagementsystem.audit.service.AuditService;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.visa.dto.CreateVisaDTO;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.example.visacasemanagementsystem.visa.mapper.VisaMapper;
import org.example.visacasemanagementsystem.visa.repository.VisaRepository;
import org.example.visacasemanagementsystem.visa.service.VisaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisaServiceTest {

    @Mock
    private VisaRepository visaRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VisaMapper visaMapper;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private VisaService visaService;

    // Alla find metoder?

    // applyForVisa 3x
    @Test
    void applyForVisa_shouldSaveVisa_WhenDataIsValid() {
        // Arrange
        Long userId = 1L;
        CreateVisaDTO dto = new CreateVisaDTO(
                VisaType.STUDY, "Swedish","AB123",
                LocalDate.now(),
                userId
        );

        User user = new User();
        user.setId(userId);

        Visa visa = new Visa();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(visaMapper.toEntity(dto)).thenReturn(visa);
        when(visaRepository.save(any(Visa.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        visaService.applyForVisa(dto,userId);

        // Assert
        verify(visaRepository, times(1)).save(any(Visa.class));
        verify(auditService, times(1)).createAuditLog(eq(userId), any(), any(),any());
        assertThat(visa.getVisaStatus()).isEqualTo(VisaStatus.SUBMITTED);
        assertThat(visa.getApplicant()).isEqualTo(user);
    }

    @Test
    void applyForVisa_shouldThrowIllegalArgumentException_WhenTravelDateIsInThePast() {
        // Arrange
        Long userId = 1L;
        CreateVisaDTO dto = new CreateVisaDTO(
                VisaType.STUDY, "Swedish","AB123",
                LocalDate.now().minusDays(1),
                userId
        );

        // Act & Assert
        assertThatThrownBy(()-> visaService.applyForVisa(dto, userId))
        .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Travel date cannot be in the past.");

        // Verify
        verifyNoInteractions(visaRepository);
        verifyNoInteractions(auditService);
    }

    @Test
    void applyForVisa_shouldThrowException_WhenUserIsNotFoundInDb() {
        Long userId = 99L;
        CreateVisaDTO dto = new CreateVisaDTO(
                VisaType.STUDY, "Swedish","AB123",
                LocalDate.now(),
                userId
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(()-> visaService.applyForVisa(dto, userId))
        .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
    }

    // approveVisa_shouldApproveVisa_andCreateLog



    // rejectVisa 2x
    @Test
    void rejectVisa_shouldUpdateStatus_AndCreateLog() {
        // Arrange
        Long visaId = 1L;
        Long adminId = 2L;
        String reason = "Missing documents";

        User admin = new User();
        admin.setUserAuthorization(UserAuthorization.ADMIN);

        Visa visa = new Visa();
        visa.setVisaStatus(VisaStatus.SUBMITTED);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(visaRepository.findById(visaId)).thenReturn(Optional.of(visa));
        when(visaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        visaService.rejectVisa(visaId, adminId, reason);

        // Assert
        assertThat(visa.getVisaStatus()).isEqualTo(VisaStatus.REJECTED);
        assertThat(visa.getStatusInformation()).isEqualTo(reason);
        verify(auditService).createAuditLog(eq(adminId), eq(visaId), eq(AuditEventType.REJECTED), contains(reason));

    }

    @Test
    void rejectVisa_shouldThrowIllegalArgumentException_WhenRejectReasonIsMissing() {
        // Arrange
        Long visaId = 1L;
        Long adminId = 2L;
        String missingReason = " ";

        // Act & Assert
        assertThatThrownBy(() -> visaService.rejectVisa(visaId, adminId, missingReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reason for rejection cannot be null or blank");

        verifyNoInteractions(visaRepository);
        verifyNoInteractions(auditService);
    }

    // requestMoreInformation_shouldUpdateStatusAndCreateLog
    // assignHandler_shouldAssignHandlerToVisaCase_AndCreateLog

    // ValidateHandler 3x
    @Test
    void validateHandler_shouldReturnUser_whenUserIsAdmin() {
        // Arrange
        Long adminId = 1L;
        User admin = new User();
        admin.setUserAuthorization(UserAuthorization.ADMIN);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        // Act
        User result = visaService.validateHandler(adminId);

        // Assert
        assertThat(result).isEqualTo(admin);
    }

    @Test
    void validateHandler_shouldThrowUnauthorizedException_WhenUserIsApplicant() {
        // Arrange
        Long userId = 1L;
        User applicant = new User();
        applicant.setUserAuthorization(UserAuthorization.USER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(applicant));

        assertThatThrownBy(()-> visaService.validateHandler(userId))
                    .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User is not authorized to perform this action.");
    }

    @Test
    void validateHandler_shouldThrowEntityNotFoundException_WhenUserIsApplicant() {
        // Arrange
        Long nonExistingUserId = 999L;

        when(userRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(()-> visaService.validateHandler(nonExistingUserId))
        .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found.");

        verifyNoInteractions(visaRepository);
    }





}
