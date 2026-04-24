package org.example.visacasemanagementsystem.audit.mapper;


import org.example.visacasemanagementsystem.audit.FileEventType;
import org.example.visacasemanagementsystem.audit.dto.FileLogDTO;
import org.example.visacasemanagementsystem.audit.entity.FileLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

@DisplayName("FileLogMapper unit tests")
class FileLogMapperTest {

    private FileLogMapper fileLogMapper;

    @BeforeEach
    void setUp() {
        fileLogMapper = new FileLogMapper();
    }

    @Test
    @DisplayName("Checking if toDTO copies every field from FileLog entity to FileLogDTO")
    void toDTO_shouldCopyAllFields_WhenEntityIsPopulated() {
        // Arrange
        LocalDateTime timestamp = LocalDateTime.of(2026, 4, 24, 15, 0, 0);

        FileLog entity = new FileLog();
        entity.setId(101L);
        entity.setTimeStamp(timestamp);
        entity.setActorUserId(7L);
        entity.setVisaCaseId(200L);
        entity.setFileName("passport_scan.pdf");
        entity.setFileEventType(FileEventType.UPLOADED);
        entity.setDescription("Initial passport document uploaded.");

        // Act
        FileLogDTO dto =  fileLogMapper.toDTO(entity);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(101L);
        assertThat(dto.timeStamp()).isEqualTo(timestamp);
        assertThat(dto.actorUserId()).isEqualTo(7L);
        assertThat(dto.visaCaseId()).isEqualTo(200L);
        assertThat(dto.fileName()).isEqualTo("passport_scan.pdf");
        assertThat(dto.fileEventType()).isEqualTo(FileEventType.UPLOADED);
        assertThat(dto.description()).isEqualTo("Initial passport document uploaded.");

    }

    @Test
    @DisplayName("Checking if toDTO returns null when entity is null")
    void toDTO_shouldReturnNull_WhenEntityIsNull() {
        // Act
        FileLogDTO dto = fileLogMapper.toDTO(null);

        // Assert
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("Checking if toEntity sets all fields correctly for a new file log")
    void toEntity_shouldSetAllFields_WhenAllArgumentsAreProvided() {
        // Arrange
        Long actorId = 7L;
        Long visaId = 200L;
        String fileName = "visa_photo.jpg";
        String description = "Applicant photo uploaded via web portal.";

        // Act
        FileLog entity = fileLogMapper.toEntity(
                actorId,
                visaId,
                fileName,
                FileEventType.UPLOADED,
                description
        );

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getActorUserId()).isEqualTo(actorId);
        assertThat(entity.getVisaCaseId()).isEqualTo(visaId);
        assertThat(entity.getFileName()).isEqualTo("visa_photo.jpg");
        assertThat(entity.getFileEventType()).isEqualTo(FileEventType.UPLOADED);
        assertThat(entity.getDescription()).isEqualTo(description);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getTimeStamp()).isNull();

    }

    @Test
    @DisplayName("Checking if toEntity handles deletion events correctly")
    void toEntity_shouldHandleDeletionEventType() {
        // Act
        FileLog entity = fileLogMapper.toEntity(
                1L,
                200L,
                "old_document.pdf",
                FileEventType.DELETED,
                "Document removed due to incorrect format"
        );

        // Assert
        assertThat(entity.getFileEventType()).isEqualTo(FileEventType.DELETED);
        assertThat(entity.getFileName()).isEqualTo("old_document.pdf");
    }


}
