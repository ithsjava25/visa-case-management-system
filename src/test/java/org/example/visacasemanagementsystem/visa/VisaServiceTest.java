package org.example.visacasemanagementsystem.visa;

import jakarta.persistence.EntityNotFoundException;
import org.example.visacasemanagementsystem.audit.AuditEventType;
import org.example.visacasemanagementsystem.audit.service.AuditService;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.visa.dto.CreateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.UpdateVisaDTO;
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

    // Find metoder?

    // applyForVisa 3x
    @Test
    void applyForVisa_shouldSaveVisa_WhenDataIsValid() {
        // Arrange
        Long userId = 1L;
        CreateVisaDTO dto = new CreateVisaDTO(
                VisaType.STUDY, "Swedish", "AB123",
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
        visaService.applyForVisa(dto, userId);

        // Assert
        verify(visaRepository, times(1)).save(any(Visa.class));
        verify(auditService, times(1)).createAuditLog(eq(userId), any(), any(), any());
        assertThat(visa.getVisaStatus()).isEqualTo(VisaStatus.SUBMITTED);
        assertThat(visa.getApplicant()).isEqualTo(user);
    }

    @Test
    void applyForVisa_shouldThrowIllegalArgumentException_WhenTravelDateIsInThePast() {
        // Arrange
        Long userId = 1L;
        CreateVisaDTO dto = new CreateVisaDTO(
                VisaType.STUDY, "Swedish", "AB123",
                LocalDate.now().minusDays(1),
                userId
        );

        // Act & Assert
        assertThatThrownBy(() -> visaService.applyForVisa(dto, userId))
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
                VisaType.STUDY, "Swedish", "AB123",
                LocalDate.now(),
                userId
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> visaService.applyForVisa(dto, userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
    }

    // updateVisa 3x
    @Test
    void updateVisa_shouldUpdateVisaAndResetStatus_WhenUserIsAuthorized() {
        // Arrange
        Long userId = 1L;
        Long visaId = 100L;

        UpdateVisaDTO dto = new UpdateVisaDTO(
                visaId, VisaType.TOURIST, VisaStatus.SUBMITTED,
                "Swedish", "XYZ789", LocalDate.now().plusMonths(1), null
        );

        User applicant = new User();
        applicant.setId(userId);

        Visa visa = new Visa();
        visa.setId(visaId);
        visa.setApplicant(applicant);
        visa.setVisaStatus(VisaStatus.INCOMPLETE);
        visa.setStatusInformation("PLease clarify travel purpose");

        when(visaRepository.findById(visaId)).thenReturn(Optional.of(visa));
        when(visaRepository.save(any(Visa.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        visaService.updateVisa(visaId, dto, userId );

        // Assert
        assertThat(visa.getVisaType()).isEqualTo(VisaType.TOURIST);
        assertThat(visa.getPassportNumber()).isEqualTo("XYZ789");
        assertThat(visa.getVisaStatus()).isEqualTo(VisaStatus.SUBMITTED);
        assertThat(visa.getStatusInformation()).isNull();

        verify(auditService).createAuditLog(eq(userId), eq(visaId), eq(AuditEventType.UPDATED), anyString());

    }

    @Test
    void updateVisa_shouldThrowUnauthorizedException_WhenUserIsNotTheApplicant() {
        // Arrange
        Long visaId = 100L;
        Long actualApplicantId = 3L;
        Long unauthorizedUserId = 99L;

        UpdateVisaDTO dto = new UpdateVisaDTO(100L, VisaType.EMPLOYMENT,
                VisaStatus.SUBMITTED, "US", "123",
                LocalDate.now().plusDays(1), null);

        User actualApplicant = new User();
        actualApplicant.setId(actualApplicantId);

        Visa visa = new Visa();
        visa.setApplicant(actualApplicant);

        when(visaRepository.findById(anyLong())).thenReturn(Optional.of(visa));

        // Act & Assert
        assertThatThrownBy(() -> visaService.updateVisa(visaId, dto,unauthorizedUserId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You are not authorized to update this application.");

    }

    @Test
    void updateVisa_shouldThrowIllegalArgumentException_WhenStatusIsNotEditable() {
        Long visaId = 100L;
        Long userId = 1L;
        UpdateVisaDTO dto = new UpdateVisaDTO(
                visaId, VisaType.STUDY,
                VisaStatus.SUBMITTED, "SE", "123",
                LocalDate.now().plusDays(10), null);

        User user  = new User();
        user.setId(userId);

        Visa visa = new Visa();
        visa.setApplicant(user);
        visa.setVisaStatus(VisaStatus.GRANTED);

        when(visaRepository.findById(anyLong())).thenReturn(Optional.of(visa));

        // Act & Assert
        assertThatThrownBy(() -> visaService.updateVisa(visaId, dto,userId))
        .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This application can no longer be edited.");
    }


    @Test
    void updateVisa_shouldThrowIllegalArgumentException_WhenTravelDateIsInPast() {
        // Arrange
        Long visaId = 100L;
        Long userId = 1L;
        LocalDate pastDate = LocalDate.now().minusDays(1);

        UpdateVisaDTO dto = new UpdateVisaDTO(100L, VisaType.STUDY,
                VisaStatus.SUBMITTED, "Swedish", "CDE789", pastDate, null);

        User actualUser = new User();
        actualUser.setId(userId);

        Visa visa = new Visa();
        visa.setApplicant(actualUser);
        visa.setVisaStatus(VisaStatus.SUBMITTED);

        when(visaRepository.findById(anyLong())).thenReturn(Optional.of(visa));

        // Act & Assert
        assertThatThrownBy(() -> visaService.updateVisa(visaId, dto, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Travel date cannot be in the past.");

    }

    @Test
    void approveVisa_shouldApproveVisa_andCreateLog() {
        // Arrange
        Long visaId = 1L;
        Long adminId = 2L;

        User admin = new User();
        admin.setId(adminId);
        admin.setUserAuthorization(UserAuthorization.ADMIN);

        Visa visa = new Visa();
        visa.setId(visaId);
        visa.setVisaStatus(VisaStatus.SUBMITTED);
        visa.setApplicant(null);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(visaRepository.findById(visaId)).thenReturn(Optional.of(visa));
        when(visaRepository.save(any(Visa.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        visaService.approveVisa(visaId, adminId);

        // Assert
        assertThat(visa.getVisaStatus()).isEqualTo(VisaStatus.GRANTED);
        assertThat(visa.getStatusInformation()).isNull();
        assertThat(visa.getHandler()).isEqualTo(admin);

        verify(visaRepository, times(1)).save(visa);
        verify(auditService).createAuditLog(eq(adminId), eq(visaId), eq(AuditEventType.GRANTED),
                contains("granted"));

    }

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

    @Test
    void requestMoreInformation_shouldUpdateStatusAndInfoText_AndCreateLog() {
        // Arrange
        Long visaId = 1L;
        Long adminId = 2L;
        String infoText = "Please upload a clearer picture of your passport";

        User admin = new User();
        admin.setUserAuthorization(UserAuthorization.ADMIN);

        Visa visa = new Visa();
        visa.setVisaStatus(VisaStatus.SUBMITTED);
        visa.setHandler(null);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(visaRepository.findById(visaId)).thenReturn(Optional.of(visa));
        when(visaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        visaService.requestMoreInformation(visaId, adminId, infoText);

        // Assert
        assertThat(visa.getVisaStatus()).isEqualTo(VisaStatus.INCOMPLETE);
        assertThat(visa.getStatusInformation()).isEqualTo(infoText);
        assertThat(visa.getHandler()).isEqualTo(admin);

        verify(auditService).createAuditLog(
                eq(adminId),
                eq(visaId),
                eq(AuditEventType.UPDATED),
                contains(infoText));
    }

    @Test
    void assignHandler_shouldAssignAdminToVisa_AndChangeStatusToAssigned() {
        // Arrange
        Long visaId = 1L;
        Long adminId = 2L;

        User  admin = new User();
        admin.setFullName("Test Admin");
        admin.setUserAuthorization(UserAuthorization.ADMIN);

        Visa visa = new Visa();
        visa.setVisaStatus(VisaStatus.SUBMITTED);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(visaRepository.findById(visaId)).thenReturn(Optional.of(visa));
        when(visaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        visaService.assignHandler(visaId, adminId);

        // Assert
        assertThat(visa.getHandler()).isEqualTo(admin);
        assertThat(visa.getVisaStatus()).isEqualTo(VisaStatus.ASSIGNED);

        verify(auditService).createAuditLog(
                eq(adminId),
                eq(visaId),
                eq(AuditEventType.ASSIGNED),
                contains("Test Admin"));
    }

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

        assertThatThrownBy(() -> visaService.validateHandler(userId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User is not authorized to perform this action.");
    }

    @Test
    void validateHandler_shouldThrowEntityNotFoundException_WhenUserIsApplicant() {
        // Arrange
        Long nonExistingUserId = 999L;

        when(userRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> visaService.validateHandler(nonExistingUserId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found.");

        verifyNoInteractions(visaRepository);
    }


}
