package org.example.visacasemanagementsystem.visa;

import jakarta.persistence.EntityNotFoundException;
import org.example.visacasemanagementsystem.audit.VisaEventType;
import org.example.visacasemanagementsystem.audit.service.VisaLogService;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.visa.dto.CreateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.UpdateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.VisaDTO;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.example.visacasemanagementsystem.visa.mapper.VisaMapper;
import org.example.visacasemanagementsystem.visa.repository.VisaRepository;
import org.example.visacasemanagementsystem.visa.service.VisaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
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
    private VisaLogService visaLogService;

    @InjectMocks
    private VisaService visaService;

    @Test
    void applyForVisa_shouldThrowIllegalArgumentException_WhenTravelDateIsInThePast() {
        // Arrange
        Long userId = 1L;
        CreateVisaDTO dto = new CreateVisaDTO(
                VisaType.STUDY, "Swedish", "AB123",
                LocalDate.now().minusDays(1)
        );

        // Act & Assert
        assertThatThrownBy(() -> visaService.applyForVisa(dto, userId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Travel date cannot be in the past.");

        // Verify
        verifyNoInteractions(visaRepository);
        verifyNoInteractions(visaLogService);
    }

    @Test
    void applyForVisa_shouldThrowException_WhenUserIsNotFoundInDb() {
        Long userId = 99L;
        CreateVisaDTO dto = new CreateVisaDTO(
                VisaType.STUDY, "Swedish", "AB123",
                LocalDate.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> visaService.applyForVisa(dto, userId, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void updateVisa_shouldThrowUnauthorizedException_WhenUserIsNotTheApplicant() {
        // Arrange
        Long visaId = 100L;
        Long actualApplicantId = 3L;
        Long unauthorizedUserId = 99L;

        UpdateVisaDTO dto = new UpdateVisaDTO(100L, VisaType.EMPLOYMENT,
                VisaStatus.SUBMITTED, "US", "123",
                LocalDate.now().plusDays(1), null, null);

        User actualApplicant = new User();
        actualApplicant.setId(actualApplicantId);

        Visa visa = new Visa();
        visa.setApplicant(actualApplicant);

        when(visaRepository.findById(anyLong())).thenReturn(Optional.of(visa));

        // Act & Assert
        assertThatThrownBy(() -> visaService.updateVisa(visaId, dto,unauthorizedUserId, null))
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
                LocalDate.now().plusDays(10), null, null);

        User user  = new User();
        user.setId(userId);

        Visa visa = new Visa();
        visa.setApplicant(user);
        visa.setVisaStatus(VisaStatus.GRANTED);

        when(visaRepository.findById(anyLong())).thenReturn(Optional.of(visa));

        // Act & Assert
        assertThatThrownBy(() -> visaService.updateVisa(visaId, dto,userId, null))
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
                VisaStatus.SUBMITTED, "Swedish", "CDE789", pastDate, null, null);

        User actualUser = new User();
        actualUser.setId(userId);

        Visa visa = new Visa();
        visa.setApplicant(actualUser);
        visa.setVisaStatus(VisaStatus.SUBMITTED);

        when(visaRepository.findById(anyLong())).thenReturn(Optional.of(visa));

        // Act & Assert
        assertThatThrownBy(() -> visaService.updateVisa(visaId, dto, userId, null))
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
        verify(visaLogService).createVisaLog(eq(adminId), eq(visaId), eq(VisaEventType.GRANTED),
                contains("granted"));

    }

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
        verify(visaLogService).createVisaLog(eq(adminId), eq(visaId), eq(VisaEventType.REJECTED), contains(reason));

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
        verifyNoInteractions(visaLogService);
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

        verify(visaLogService).createVisaLog(
                eq(adminId),
                eq(visaId),
                eq(VisaEventType.UPDATED),
                contains(infoText));
    }

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

    // --- Helpers ---

    private static VisaDTO visaDtoWithId(Long id) {
        return new VisaDTO(id, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    // --- Tests for the /visa/cases landing-page queries ---

    @Test
    void findOpenCasesByHandler_shouldMapEntitiesToDTOs_andQueryAssignedAndIncomplete() {
        // Arrange
        Long handlerId = 5L;
        List<VisaStatus> expectedStatuses = List.of(VisaStatus.ASSIGNED, VisaStatus.INCOMPLETE);
        Sort expectedSort = Sort.by("updatedAt").descending();

        Visa v1 = new Visa();
        v1.setId(1L);
        Visa v2 = new Visa();
        v2.setId(2L);
        VisaDTO d1 = visaDtoWithId(1L);
        VisaDTO d2 = visaDtoWithId(2L);

        when(visaRepository.findByHandler_IdAndVisaStatusIn(
                eq(handlerId), eq(expectedStatuses), eq(expectedSort)))
                .thenReturn(List.of(v1, v2));
        when(visaMapper.toDTO(v1)).thenReturn(d1);
        when(visaMapper.toDTO(v2)).thenReturn(d2);

        // Act
        List<VisaDTO> result = visaService.findOpenCasesByHandler(handlerId);

        // Assert
        assertThat(result).containsExactly(d1, d2);
        verify(visaRepository).findByHandler_IdAndVisaStatusIn(
                handlerId, expectedStatuses, expectedSort);
        verifyNoMoreInteractions(visaRepository);
    }

    @Test
    void findOpenCasesByHandler_shouldReturnEmptyList_whenHandlerHasNoOpenCases() {
        // Arrange
        Long handlerId = 5L;
        when(visaRepository.findByHandler_IdAndVisaStatusIn(
                anyLong(), anyList(), any(Sort.class)))
                .thenReturn(List.of());

        // Act
        List<VisaDTO> result = visaService.findOpenCasesByHandler(handlerId);

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(visaMapper);
    }

    @Test
    void findUnassignedCases_shouldMapEntitiesToDTOs_andQuerySubmittedWithNullHandler() {
        // Arrange
        Sort expectedSort = Sort.by("updatedAt").descending();

        Visa v1 = new Visa();
        VisaDTO d1 = visaDtoWithId(10L);

        when(visaRepository.findByVisaStatusAndHandlerIsNull(
                eq(VisaStatus.SUBMITTED), eq(expectedSort)))
                .thenReturn(List.of(v1));
        when(visaMapper.toDTO(v1)).thenReturn(d1);

        // Act
        List<VisaDTO> result = visaService.findUnassignedCases();

        // Assert
        assertThat(result).containsExactly(d1);
        verify(visaRepository).findByVisaStatusAndHandlerIsNull(
                VisaStatus.SUBMITTED, expectedSort);
        verifyNoMoreInteractions(visaRepository);
    }

    @Test
    void findUnassignedCases_shouldReturnEmptyList_whenNoSubmittedCasesExist() {
        // Arrange
        when(visaRepository.findByVisaStatusAndHandlerIsNull(
                any(VisaStatus.class), any(Sort.class)))
                .thenReturn(List.of());

        // Act
        List<VisaDTO> result = visaService.findUnassignedCases();

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(visaMapper);
    }

    @Test
    void findHandledCasesByHandler_shouldMapEntitiesToDTOs_andQueryGrantedAndRejected() {
        // Arrange
        Long handlerId = 7L;
        List<VisaStatus> expectedStatuses = List.of(VisaStatus.GRANTED, VisaStatus.REJECTED);
        Sort expectedSort = Sort.by("updatedAt").descending();

        Visa granted = new Visa();
        granted.setId(100L);
        Visa rejected = new Visa();
        rejected.setId(101L);
        VisaDTO grantedDto = visaDtoWithId(100L);
        VisaDTO rejectedDto = visaDtoWithId(101L);

        when(visaRepository.findByHandler_IdAndVisaStatusIn(
                eq(handlerId), eq(expectedStatuses), eq(expectedSort)))
                .thenReturn(List.of(granted, rejected));
        when(visaMapper.toDTO(granted)).thenReturn(grantedDto);
        when(visaMapper.toDTO(rejected)).thenReturn(rejectedDto);

        // Act
        List<VisaDTO> result = visaService.findHandledCasesByHandler(handlerId);

        // Assert
        assertThat(result).containsExactly(grantedDto, rejectedDto);
        verify(visaRepository).findByHandler_IdAndVisaStatusIn(
                handlerId, expectedStatuses, expectedSort);
        verifyNoMoreInteractions(visaRepository);
    }

    @Test
    void findHandledCasesByHandler_shouldReturnEmptyList_whenHandlerHasClosedNothing() {
        // Arrange
        Long handlerId = 7L;
        when(visaRepository.findByHandler_IdAndVisaStatusIn(
                anyLong(), anyList(), any(Sort.class)))
                .thenReturn(List.of());

        // Act
        List<VisaDTO> result = visaService.findHandledCasesByHandler(handlerId);

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(visaMapper);
    }

}
