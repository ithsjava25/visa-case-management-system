package org.example.visacasemanagementsystem.visa;

import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.visa.dto.CreateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.UpdateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.VisaDTO;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.example.visacasemanagementsystem.visa.mapper.VisaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class VisaMapperTest {

    private VisaMapper visaMapper;

    @BeforeEach
    void setUp() {
        visaMapper = new VisaMapper();
    }

    @Test
    void shouldMapCreateVisaDTOtoVisaEntity() {
        // Arrange
        CreateVisaDTO dto = new CreateVisaDTO(
                VisaType.STUDY,
                "Swedish",
                "AB123456",
                LocalDate.now().plusMonths(3)
        );

        // Arrange
        Visa result = visaMapper.toEntity(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getVisaType()).isEqualTo(VisaType.STUDY);
        assertThat(result.getNationality()).isEqualTo("Swedish");
        assertThat(result.getPassportNumber()).isEqualTo("AB123456");
        assertThat(result.getTravelDate()).isEqualTo(dto.travelDate());

    }

    @Test
    void shouldMapVisaEntityToVisaDTO() {
        // Arrange
        User applicant = new User();
        applicant.setFullName("Test user");

      Visa visa  = new Visa();
      visa.setId(100L);
      visa.setVisaType(VisaType.EMPLOYMENT);
      visa.setNationality("English");
      visa.setPassportNumber("EN123456");
      visa.setTravelDate(LocalDate.now().plusWeeks(2));
      visa.setApplicant(applicant);

      // Act
        VisaDTO dto = visaMapper.toDTO(visa);

        // Assert
        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.visaType()).isEqualTo(VisaType.EMPLOYMENT);
        assertThat(dto.applicantName()).isEqualTo("Test user");
        assertThat(dto.nationality()).isEqualTo("English");
        assertThat(dto.travelDate()).isEqualTo(visa.getTravelDate());
        assertThat(dto.passportNumber()).isEqualTo("EN123456");
    }

    @Test
    void shouldUpdateExistingVisaEntityFromUpdateVisaDTO() {
        // Arrange
        Visa existingVisa = new Visa();
        existingVisa.setNationality("Old Nationality");
        existingVisa.setPassportNumber("OLD-123");

        UpdateVisaDTO updateDto = new UpdateVisaDTO(
                1L,
                VisaType.TOURIST,
                VisaStatus.ASSIGNED,
                "New Nationality",
                "NEW-456",
                LocalDate.now().plusMonths(1),
                2L,
                null
        );

        // Act
        visaMapper.updateEntityFromDTO(updateDto, existingVisa);

        // Assert
        assertThat(existingVisa.getNationality()).isEqualTo("New Nationality");
        assertThat(existingVisa.getPassportNumber()).isEqualTo("NEW-456");
        assertThat(existingVisa.getTravelDate()).isEqualTo(updateDto.travelDate());
        assertThat(existingVisa.getVisaType()).isEqualTo(VisaType.TOURIST);


    }
}
