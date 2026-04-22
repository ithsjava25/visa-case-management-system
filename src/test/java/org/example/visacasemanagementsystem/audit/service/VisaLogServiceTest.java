package org.example.visacasemanagementsystem.audit.service;

import org.example.visacasemanagementsystem.audit.VisaEventType;
import org.example.visacasemanagementsystem.audit.dto.VisaLogDTO;
import org.example.visacasemanagementsystem.audit.entity.VisaLog;
import org.example.visacasemanagementsystem.audit.mapper.VisaLogMapper;
import org.example.visacasemanagementsystem.audit.repository.VisaLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VisaLogService unit tests")
class VisaLogServiceTest {

    @Mock
    private VisaLogRepository visaLogRepository;
    @Mock
    private VisaLogMapper visaLogMapper;

    @InjectMocks
    private VisaLogService visaLogService;

    // ── createVisaLog ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if createVisaLog delegates to the mapper and persists the resulting entity")
    void createVisaLog_shouldMapAndSaveEntity_WhenCalledWithValidArguments() {
        // Arrange
        Long userId = 7L;
        Long visaCaseId = 100L;
        VisaEventType eventType = VisaEventType.UPDATED;
        String description = "Comment added by Test User";

        VisaLog mappedEntity = new VisaLog();
        mappedEntity.setUserId(userId);
        mappedEntity.setVisaCaseId(visaCaseId);
        mappedEntity.setVisaEventType(eventType);
        mappedEntity.setDescription(description);

        when(visaLogMapper.toEntity(userId, visaCaseId, eventType, description))
                .thenReturn(mappedEntity);

        // Act
        visaLogService.createVisaLog(userId, visaCaseId, eventType, description);

        // Assert
        verify(visaLogMapper).toEntity(userId, visaCaseId, eventType, description);
        verify(visaLogRepository).save(mappedEntity);
        verifyNoMoreInteractions(visaLogRepository);
    }

    @Test
    @DisplayName("Checking if createVisaLog passes the exact same entity instance from mapper to repository")
    void createVisaLog_shouldPassSameEntityInstance_WhenSavingToRepository() {
        // Arrange
        VisaLog mappedEntity = new VisaLog();
        when(visaLogMapper.toEntity(any(), any(), any(), any()))
                .thenReturn(mappedEntity);

        // Act
        visaLogService.createVisaLog(1L, 1L, VisaEventType.CREATED, "x");

        // Assert
        ArgumentCaptor<VisaLog> captor = ArgumentCaptor.forClass(VisaLog.class);
        verify(visaLogRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(mappedEntity);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if findAll returns mapped DTOs in the order returned by the repository")
    void findAll_shouldReturnAllLogsAsDTOs_WhenRepositoryHasEntries() {
        // Arrange
        VisaLog firstEntity = new VisaLog();
        firstEntity.setId(1L);
        VisaLog secondEntity = new VisaLog();
        secondEntity.setId(2L);

        VisaLogDTO firstDTO = new VisaLogDTO(
                1L, LocalDateTime.now(), 1L, 10L, VisaEventType.CREATED, "first");
        VisaLogDTO secondDTO = new VisaLogDTO(
                2L, LocalDateTime.now(), 2L, 20L, VisaEventType.GRANTED, "second");

        when(visaLogRepository.findAll()).thenReturn(List.of(firstEntity, secondEntity));
        when(visaLogMapper.toDTO(firstEntity)).thenReturn(firstDTO);
        when(visaLogMapper.toDTO(secondEntity)).thenReturn(secondDTO);

        // Act
        List<VisaLogDTO> result = visaLogService.findAll();

        // Assert
        assertThat(result).containsExactly(firstDTO, secondDTO);
        verify(visaLogRepository).findAll();
        verify(visaLogMapper).toDTO(firstEntity);
        verify(visaLogMapper).toDTO(secondEntity);
    }

    @Test
    @DisplayName("Checking if findAll returns an empty list when repository has no entries")
    void findAll_shouldReturnEmptyList_WhenRepositoryIsEmpty() {
        // Arrange
        when(visaLogRepository.findAll()).thenReturn(List.of());

        // Act
        List<VisaLogDTO> result = visaLogService.findAll();

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(visaLogMapper);
    }
}
