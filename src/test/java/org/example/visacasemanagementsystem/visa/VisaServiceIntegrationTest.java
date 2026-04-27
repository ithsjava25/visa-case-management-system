package org.example.visacasemanagementsystem.visa;

import org.example.visacasemanagementsystem.audit.VisaEventType;
import org.example.visacasemanagementsystem.audit.service.VisaLogService;
import org.example.visacasemanagementsystem.file.FileService;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.example.visacasemanagementsystem.visa.dto.CreateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.UpdateVisaDTO;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.example.visacasemanagementsystem.visa.repository.VisaRepository;
import org.example.visacasemanagementsystem.visa.service.VisaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class VisaServiceIntegrationTest {

    @Autowired
    private VisaService visaService;
    @Autowired
    private VisaRepository visaRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VisaLogService visaLogService;

    @MockitoBean
    private FileService fileService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void applyForVisa_shouldSaveVisa_WhenDataIsValid() {
        // Arrange
        User user = createAndSaveValidUser();
        UserPrincipal principal = authenticateUser(user);
        String testS3Key = "testS3Key";
        String passportNum = "PASS-INT-123";

        CreateVisaDTO dto = new CreateVisaDTO(
                VisaType.STUDY, "Swedish", passportNum,
                LocalDate.now().plusDays(30)
        );

        // Act
        visaService.applyForVisa(principal, dto, testS3Key);

        // Assert
        authenticateAsSysadmin();
        assertThat(visaLogService.findAll()).isNotEmpty();
        var savedVisa = visaRepository.findAll().stream()
                .filter(v -> v.getPassportNumber().equals(passportNum))
                .findFirst()
                .orElseThrow();

        assertThat(savedVisa.getNationality()).isEqualTo("Swedish");
        assertThat(savedVisa.getS3Keys()).contains(testS3Key);
    }

    @Test
    void updateVisa_shouldUpdateVisaAndResetStatus_WhenUserIsAuthorized() {
        // Arrange
        User user  = createAndSaveValidUser();
        authenticateUser(user);

        Visa visa = new Visa();
        visa.setApplicant(user);
        visa.setVisaStatus(VisaStatus.INCOMPLETE);
        visa.setPassportNumber("OLD-PASS");
        visa.setNationality("Swedish");
        visa.setVisaType(VisaType.STUDY);
        visa.setTravelDate(LocalDate.now().plusMonths(2));

        visa.setStatusInformation("Please fix upload a clearer image of your passport");
        visa = visaRepository.save(visa);

        UpdateVisaDTO dto = new UpdateVisaDTO(
                visa.getId(), VisaType.TOURIST, VisaStatus.SUBMITTED,
                "Swedish", "NEW-PASS-999", LocalDate.now().plusMonths(1), null, null
        );

        // Act
        visaService.updateVisa(visa.getId(), dto, user.getId(), null);

        // Assert
        Visa updatedVisa = visaRepository.findById(visa.getId()).get();

        assertThat(updatedVisa.getPassportNumber()).isEqualTo("NEW-PASS-999");
        assertThat(updatedVisa.getVisaType()).isEqualTo(VisaType.TOURIST);
        assertThat(updatedVisa.getVisaStatus()).isEqualTo(VisaStatus.SUBMITTED);
        assertThat(updatedVisa.getStatusInformation()).isNull();

        authenticateAsSysadmin();
        var logs = visaLogService.findAll();

        Visa finalVisa = visa;
        assertThat(logs)
                .as("Check that an update log exists in the audit DTO list")
                .anyMatch(log ->
                        log.visaCaseId() != null &&
                                log.visaCaseId().equals(finalVisa.getId()) &&
                                log.visaEventType() == VisaEventType.UPDATED
                );

    }

    @Test
    void assignHandler_shouldAssignAdminToVisa_AndChangeStatusToAssigned() {
        // Arrange
        User admin = new User();
        String adminEmail = "admin@test2.com";
        admin.setFullName("Test Admin");
        admin.setEmail(adminEmail);
        admin.setUsername(adminEmail);
        admin.setPassword("password123");
        admin.setUserAuthorization(UserAuthorization.ADMIN);
        admin = userRepository.save(admin);
        authenticateUser(admin);

        User applicant = createAndSaveValidUser();

        Visa visa = new Visa();
        visa.setApplicant(applicant);
        visa.setVisaStatus(VisaStatus.INCOMPLETE);
        visa.setNationality("Swedish");
        visa.setPassportNumber("PASS123");
        visa.setVisaType(VisaType.STUDY);
        visa.setTravelDate(LocalDate.now().plusMonths(1));
        visa = visaRepository.save(visa);

        // Act
        visaService.assignHandler(visa.getId(), admin.getId());

        // Assert
        Visa updatedVisa = visaRepository.findById(visa.getId()).orElseThrow();

        assertThat(updatedVisa.getHandler()).isNotNull();
        assertThat(updatedVisa.getHandler().getId()).isEqualTo(admin.getId());
        assertThat(updatedVisa.getVisaStatus()).isEqualTo(VisaStatus.ASSIGNED);

        authenticateAsSysadmin();
        var logs = visaLogService.findAll();
        User finalAdmin = admin;
        assertThat(logs).anyMatch(log ->
                log.visaCaseId().equals(updatedVisa.getId()) &&
                        log.userId().equals(finalAdmin.getId()) &&
                        log.visaEventType() == VisaEventType.ASSIGNED &&
                        log.description().contains("Test Admin")
        );
    }


    private User createAndSaveValidUser() {
        User user = new User();
        String testEmail = java.util.UUID.randomUUID() + "@test.com"; // Unik mail varje gång
        user.setFullName("Test User");
        user.setEmail(testEmail);
        user.setUsername(testEmail);
        user.setPassword("password");
        user.setUserAuthorization(UserAuthorization.USER);

        return userRepository.save(user);
    }

    private UserPrincipal authenticateUser(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        Authentication authentication = new TestingAuthenticationToken(principal, "password", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return principal;
    }

    /**
     * Switches the security context to a SYSADMIN user so that calls to
     * visaLogService.findAll() (which requires SYSADMIN) succeed during
     * log-assertion steps without affecting the action under test.
     */
    private void authenticateAsSysadmin() {
        User sysadmin = new User();
        String email = java.util.UUID.randomUUID() + "@sysadmin-test.com";
        sysadmin.setFullName("Test Sysadmin");
        sysadmin.setEmail(email);
        sysadmin.setUsername(email);
        sysadmin.setPassword("password");
        sysadmin.setUserAuthorization(UserAuthorization.SYSADMIN);
        sysadmin = userRepository.save(sysadmin);
        authenticateUser(sysadmin);
    }

}
