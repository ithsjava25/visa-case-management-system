package org.example.visacasemanagementsystem.visa;

import org.example.visacasemanagementsystem.audit.AuditEventType;
import org.example.visacasemanagementsystem.audit.service.AuditService;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.visa.dto.CreateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.UpdateVisaDTO;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.example.visacasemanagementsystem.visa.repository.VisaRepository;
import org.example.visacasemanagementsystem.visa.service.VisaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
    private AuditService auditService;

    @Test
    void applyForVisa_shouldSaveVisa_WhenDataIsValid() {
        // Arrange
       User user = createAndSaveValidUser();

        CreateVisaDTO dto = new CreateVisaDTO(
                VisaType.STUDY, "Swedish", "PASS-INT-123",
                LocalDate.now().plusDays(30),
                user.getId()
        );

        // Act
        visaService.applyForVisa(dto, user.getId());

        // Assert
        var savedVisas = visaRepository.findAll();
        assertThat(savedVisas).hasSize(1);
        assertThat(savedVisas.get(0).getPassportNumber()).isEqualTo("PASS-INT-123");
        assertThat(auditService.findAll()).isNotEmpty();

    }

    @Test
    void updateVisa_shouldUpdateVisaAndResetStatus_WhenUserIsAuthorized() {
        // Arrange
        User user  = createAndSaveValidUser();

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
                "Swedish", "NEW-PASS-999", LocalDate.now().plusMonths(1), null
        );

        // Act
        visaService.updateVisa(visa.getId(), dto, user.getId());

        // Assert
      Visa updatedVisa = visaRepository.findById(visa.getId()).get();

      assertThat(updatedVisa.getPassportNumber()).isEqualTo("NEW-PASS-999");
      assertThat(updatedVisa.getVisaType()).isEqualTo(VisaType.TOURIST);
      assertThat(updatedVisa.getVisaStatus()).isEqualTo(VisaStatus.SUBMITTED);
      assertThat(updatedVisa.getStatusInformation()).isNull();

        var logs = auditService.findAll();

        Visa finalVisa = visa;
        assertThat(logs)
                .as("Check that an update log exists in the audit DTO list")
                .anyMatch(log ->
                        log.visaCaseId() != null &&
                                log.visaCaseId().equals(finalVisa.getId()) &&
                                log.auditEventType() == AuditEventType.UPDATED
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

        var logs = auditService.findAll();
        User finalAdmin = admin;
        assertThat(logs).anyMatch(log ->
                log.visaCaseId().equals(updatedVisa.getId()) &&
                        log.userId().equals(finalAdmin.getId()) &&
                        log.auditEventType() == AuditEventType.ASSIGNED &&
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



}
