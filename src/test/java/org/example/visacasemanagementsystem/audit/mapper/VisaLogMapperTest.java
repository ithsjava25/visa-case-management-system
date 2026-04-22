package org.example.visacasemanagementsystem.audit.mapper;

import org.example.visacasemanagementsystem.audit.VisaEventType;
import org.example.visacasemanagementsystem.audit.dto.VisaLogDTO;
import org.example.visacasemanagementsystem.audit.entity.VisaLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VisaLogMapper unit tests")
class VisaLogMapperTest {

    private VisaLogMapper visaLogMapper;

    @BeforeEach
    void setUp() {
        visaLogMapper = new VisaLogMapper();
    }

    // ── toDTO ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if toDTO copies every field from VisaLog entity to VisaLogDTO")
    void toDTO_shouldCopyAllFields_WhenEntityIsPopulated() {
        // Arrange
        LocalDateTime timestamp = LocalDateTime.of(2026, 4, 22, 10, 30, 0);

        VisaLog entity = new VisaLog();
        entity.setId(42L);
        entity.setTimeStamp(timestamp);
        entity.setUserId(7L);
        entity.setVisaCaseId(100L);
        entity.setVisaEventType(VisaEventType.GRANTED);
        entity.setDescription("Visa granted by handler");

        // Act
        VisaLogDTO dto = visaLogMapper.toDTO(entity);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.timeStamp()).isEqualTo(timestamp);
        assertThat(dto.userId()).isEqualTo(7L);
        assertThat(dto.visaCaseId()).isEqualTo(100L);
        assertThat(dto.visaEventType()).isEqualTo(VisaEventType.GRANTED);
        assertThat(dto.description()).isEqualTo("Visa granted by handler");
    }

    @Test
    @DisplayName("Checking if toDTO returns null when entity is null")
    void toDTO_shouldReturnNull_WhenEntityIsNull() {
        // Act
        VisaLogDTO dto = visaLogMapper.toDTO(null);

        // Assert
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("Checking if toDTO preserves a null description without throwing")
    void toDTO_shouldPreserveNullDescription_WhenDescriptionIsNull() {
        // Arrange
        VisaLog entity = new VisaLog();
        entity.setId(1L);
        entity.setTimeStamp(LocalDateTime.now());
        entity.setUserId(1L);
        entity.setVisaCaseId(1L);
        entity.setVisaEventType(VisaEventType.CREATED);
        entity.setDescription(null);

        // Act
        VisaLogDTO dto = visaLogMapper.toDTO(entity);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.description()).isNull();
    }

    // ── toEntity ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if toEntity sets all four fields and leaves id and timeStamp for JPA to fill in")
    void toEntity_shouldSetAllFields_WhenAllArgumentsAreProvided() {
        // Act
        VisaLog entity = visaLogMapper.toEntity(
                7L,
                100L,
                VisaEventType.UPDATED,
                "Status changed to APPROVED"
        );

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getUserId()).isEqualTo(7L);
        assertThat(entity.getVisaCaseId()).isEqualTo(100L);
        assertThat(entity.getVisaEventType()).isEqualTo(VisaEventType.UPDATED);
        assertThat(entity.getDescription()).isEqualTo("Status changed to APPROVED");
        // id and timeStamp are populated by JPA / @CreatedDate, not by the mapper
        assertThat(entity.getId()).isNull();
        assertThat(entity.getTimeStamp()).isNull();
    }
}
