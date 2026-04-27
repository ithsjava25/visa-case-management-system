package org.example.visacasemanagementsystem.visa;

import jakarta.persistence.EntityNotFoundException;
import org.example.visacasemanagementsystem.audit.VisaEventType;
import org.example.visacasemanagementsystem.audit.service.VisaLogService;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.example.visacasemanagementsystem.visa.dto.CreateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.UpdateVisaDTO;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.example.visacasemanagementsystem.visa.mapper.VisaMapper;
import org.example.visacasemanagementsystem.visa.repository.VisaRepository;
import org.example.visacasemanagementsystem.visa.service.VisaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
    private VisaLogService visaLogService;

    @InjectMocks
    private VisaService visaService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void applyForVisa_shouldThrowIllegalArgumentException_WhenTravelDateIsInThePast() {
        // Arrange
        User user = createAndSaveValidUser(1L, UserAuthorization.USER);
        Long userId = user.getId();
        UserPrincipal principal = authenticateUser(user);
        CreateVisaDTO dto = new CreateVisaDTO(
                VisaType.STUDY, "Swedish", "AB123",
                LocalDate.now().minusDays(1),
                userId
        );

        // Act & Assert
        assertThatThrownBy(() -> visaService.applyForVisa(principal, dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Travel date cannot be in the past.");

        // Verify
        verifyNoInteractions(visaRepository);
        verifyNoInteractions(visaLogService);
    }

    @Test
    void applyForVisa_shouldThrowException_WhenUserIsNotFoundInDb() {
        User user = new User();
        user.setId(99L);
        user.setUserAuthorization(UserAuthorization.USER);
        UserPrincipal principal = authenticateUser(user);

        CreateVisaDTO dto = new CreateVisaDTO(
                VisaType.STUDY, "Swedish", "AB123",
                LocalDate.now(),
                user.getId()
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> visaService.applyForVisa(principal, dto, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void updateVisa_shouldThrowUnauthorizedException_WhenUserIsNotTheApplicant() {
        // Arrange
        Long visaId = 100L;
        Long actualApplicantId = 3L;
        User unauthorizedUser = createAndSaveValidUser(99L, UserAuthorization.USER);
        authenticateUser(unauthorizedUser);

        UpdateVisaDTO dto = new UpdateVisaDTO(100L, VisaType.EMPLOYMENT,
                VisaStatus.SUBMITTED, "US", "123",
                LocalDate.now().plusDays(1), null, null);

        User actualApplicant = new User();
        actualApplicant.setId(actualApplicantId);

        Visa visa = new Visa();
        visa.setApplicant(actualApplicant);

        when(visaRepository.findById(anyLong())).thenReturn(Optional.of(visa));

        // Act & Assert
        assertThatThrownBy(() -> visaService.updateVisa(visaId, dto,unauthorizedUser.getId(), null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You are not authorized to update this application.");

    }

    @Test
    void updateVisa_shouldThrowIllegalArgumentException_WhenStatusIsNotEditable() {
        Long visaId = 100L;
        UpdateVisaDTO dto = new UpdateVisaDTO(
                visaId, VisaType.STUDY,
                VisaStatus.SUBMITTED, "SE", "123",
                LocalDate.now().plusDays(10), null, null);

        User user = createAndSaveValidUser(1L, UserAuthorization.USER);
        authenticateUser(user);

        Visa visa = new Visa();
        visa.setApplicant(user);
        visa.setVisaStatus(VisaStatus.GRANTED);

        when(visaRepository.findById(anyLong())).thenReturn(Optional.of(visa));

        // Act & Assert
        assertThatThrownBy(() -> visaService.updateVisa(visaId, dto, user.getId(), null))
        .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This application can no longer be edited.");
    }

    @Test
    void updateVisa_shouldThrowIllegalArgumentException_WhenTravelDateIsInPast() {
        // Arrange
        Long visaId = 100L;
        LocalDate pastDate = LocalDate.now().minusDays(1);

        UpdateVisaDTO dto = new UpdateVisaDTO(100L, VisaType.STUDY,
                VisaStatus.SUBMITTED, "Swedish", "CDE789", pastDate, null, null);

        User user = createAndSaveValidUser(1L, UserAuthorization.USER);
        authenticateUser(user);

        Visa visa = new Visa();
        visa.setApplicant(user);
        visa.setVisaStatus(VisaStatus.SUBMITTED);

        when(visaRepository.findById(anyLong())).thenReturn(Optional.of(visa));

        // Act & Assert
        assertThatThrownBy(() -> visaService.updateVisa(visaId, dto, user.getId(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Travel date cannot be in the past.");

    }

    @Test
    void approveVisa_shouldApproveVisa_andCreateLog() {
        // Arrange
        Long visaId = 1L;

        User admin = createAndSaveValidUser(2L, UserAuthorization.ADMIN);
        authenticateUser(admin);

        Visa visa = new Visa();
        visa.setId(visaId);
        visa.setVisaStatus(VisaStatus.SUBMITTED);
        visa.setApplicant(null);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(visaRepository.findById(visaId)).thenReturn(Optional.of(visa));
        when(visaRepository.save(any(Visa.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        visaService.approveVisa(visaId, admin.getId());

        // Assert
        assertThat(visa.getVisaStatus()).isEqualTo(VisaStatus.GRANTED);
        assertThat(visa.getStatusInformation()).isNull();
        assertThat(visa.getHandler()).isEqualTo(admin);

        verify(visaRepository, times(1)).save(visa);
        verify(visaLogService).createVisaLog(eq(admin.getId()), eq(visaId), eq(VisaEventType.GRANTED),
                contains("granted"));

    }

    @Test
    void rejectVisa_shouldUpdateStatus_AndCreateLog() {
        // Arrange
        Long visaId = 1L;
        String reason = "Missing documents";

        User admin = createAndSaveValidUser(2L, UserAuthorization.ADMIN);
        authenticateUser(admin);

        Visa visa = new Visa();
        visa.setVisaStatus(VisaStatus.SUBMITTED);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(visaRepository.findById(visaId)).thenReturn(Optional.of(visa));
        when(visaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        visaService.rejectVisa(visaId, admin.getId(), reason);

        // Assert
        assertThat(visa.getVisaStatus()).isEqualTo(VisaStatus.REJECTED);
        assertThat(visa.getStatusInformation()).isEqualTo(reason);
        verify(visaLogService).createVisaLog(eq(admin.getId()), eq(visaId), eq(VisaEventType.REJECTED), contains(reason));

    }

    @Test
    void rejectVisa_shouldThrowIllegalArgumentException_WhenRejectReasonIsMissing() {
        // Arrange
        Long visaId = 1L;
        String missingReason = " ";

        User admin = createAndSaveValidUser(2L, UserAuthorization.ADMIN);
        authenticateUser(admin);

        // Act & Assert
        assertThatThrownBy(() -> visaService.rejectVisa(visaId, admin.getId(), missingReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reason for rejection cannot be null or blank");

        verifyNoInteractions(visaRepository);
        verifyNoInteractions(visaLogService);
    }

    @Test
    void requestMoreInformation_shouldUpdateStatusAndInfoText_AndCreateLog() {
        // Arrange
        Long visaId = 1L;
        String infoText = "Please upload a clearer picture of your passport";

        User admin = createAndSaveValidUser(2L, UserAuthorization.ADMIN);
        authenticateUser(admin);

        Visa visa = new Visa();
        visa.setVisaStatus(VisaStatus.SUBMITTED);
        visa.setHandler(null);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(visaRepository.findById(visaId)).thenReturn(Optional.of(visa));
        when(visaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        visaService.requestMoreInformation(visaId, admin.getId(), infoText);

        // Assert
        assertThat(visa.getVisaStatus()).isEqualTo(VisaStatus.INCOMPLETE);
        assertThat(visa.getStatusInformation()).isEqualTo(infoText);
        assertThat(visa.getHandler()).isEqualTo(admin);

        verify(visaLogService).createVisaLog(
                eq(admin.getId()),
                eq(visaId),
                eq(VisaEventType.UPDATED),
                contains(infoText));
    }

    @Test
    void validateHandler_shouldReturnUser_whenUserIsAdmin() {
        // Arrange
        User admin = createAndSaveValidUser(2L, UserAuthorization.ADMIN);
        authenticateUser(admin);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        // Act
        User result = visaService.validateHandler(admin.getId());

        // Assert
        assertThat(result).isEqualTo(admin);
    }

    @Test
    void validateHandler_shouldThrowUnauthorizedException_WhenUserIsApplicant() {
        // Arrange
        User user = createAndSaveValidUser(1L, UserAuthorization.USER);
        authenticateUser(user);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> visaService.validateHandler(user.getId()))
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

    private User createAndSaveValidUser(Long userId, UserAuthorization auth) {
        User user = new User();
        String testEmail = java.util.UUID.randomUUID() + "@test.com"; // Unik mail varje gång
        user.setId(userId);
        user.setFullName("Test User");
        user.setEmail(testEmail);
        user.setUsername(testEmail);
        user.setPassword("password");
        user.setUserAuthorization(auth);
        userRepository.save(user);

        return user;
    }

    private UserPrincipal authenticateUser(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        Authentication authentication = new TestingAuthenticationToken(principal, "password", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return principal;
    }
}
