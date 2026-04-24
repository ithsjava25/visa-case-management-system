package org.example.visacasemanagementsystem.audit.service;

import org.example.visacasemanagementsystem.audit.UserEventType;
import org.example.visacasemanagementsystem.audit.dto.UserLogDTO;
import org.example.visacasemanagementsystem.audit.entity.UserLog;
import org.example.visacasemanagementsystem.audit.mapper.UserLogMapper;
import org.example.visacasemanagementsystem.audit.repository.UserLogRepository;
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
@DisplayName("UserLogService unit tests")
class UserLogServiceTest {

    @Mock
    private UserLogRepository userLogRepository;
    @Mock
    private UserLogMapper userLogMapper;

    @InjectMocks
    private UserLogService userLogService;

    // ── createUserLog ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if createUserLog delegates to the mapper and persists the resulting entity (sysadmin acting on another user)")
    void createUserLog_shouldMapAndSaveEntity_WhenActorIsNotTarget() {
        // Arrange
        Long actorUserId = 1L;
        Long targetUserId = 5L;
        UserEventType eventType = UserEventType.AUTHORIZATION_CHANGED;
        String description = "Authorization changed: USER -> ADMIN";

        UserLog mappedEntity = new UserLog();
        mappedEntity.setActorUserId(actorUserId);
        mappedEntity.setTargetUserId(targetUserId);
        mappedEntity.setUserEventType(eventType);
        mappedEntity.setDescription(description);

        when(userLogMapper.toEntity(actorUserId, targetUserId, eventType, description))
                .thenReturn(mappedEntity);

        // Act
        userLogService.createUserLog(actorUserId, targetUserId, eventType, description);

        // Assert
        verify(userLogMapper).toEntity(actorUserId, targetUserId, eventType, description);
        verify(userLogRepository).save(mappedEntity);
        verifyNoMoreInteractions(userLogRepository);
    }

    @Test
    @DisplayName("Checking if createUserLog supports actor == target (self-action like signup)")
    void createUserLog_shouldMapAndSaveEntity_WhenActorEqualsTarget() {
        // Arrange
        Long selfId = 7L;
        UserLog mappedEntity = new UserLog();
        mappedEntity.setActorUserId(selfId);
        mappedEntity.setTargetUserId(selfId);

        when(userLogMapper.toEntity(selfId, selfId, UserEventType.CREATED, "User account created via signup."))
                .thenReturn(mappedEntity);

        // Act
        userLogService.createUserLog(selfId, selfId, UserEventType.CREATED, "User account created via signup.");

        // Assert
        ArgumentCaptor<UserLog> captor = ArgumentCaptor.forClass(UserLog.class);
        verify(userLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActorUserId()).isEqualTo(captor.getValue().getTargetUserId());
    }

    @Test
    @DisplayName("Checking if createUserLog passes the exact same entity instance from mapper to repository")
    void createUserLog_shouldPassSameEntityInstance_WhenSavingToRepository() {
        // Arrange
        UserLog mappedEntity = new UserLog();
        when(userLogMapper.toEntity(any(), any(), any(), any()))
                .thenReturn(mappedEntity);

        // Act
        userLogService.createUserLog(1L, 1L, UserEventType.CREATED, "x");

        // Assert
        ArgumentCaptor<UserLog> captor = ArgumentCaptor.forClass(UserLog.class);
        verify(userLogRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(mappedEntity);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if findAll returns mapped DTOs in the order returned by the repository")
    void findAll_shouldReturnAllLogsAsDTOs_WhenRepositoryHasEntries() {
        // Arrange
        UserLog firstEntity = new UserLog();
        firstEntity.setId(1L);
        UserLog secondEntity = new UserLog();
        secondEntity.setId(2L);

        UserLogDTO firstDTO = new UserLogDTO(
                1L, LocalDateTime.now(), 1L, 1L, UserEventType.CREATED, "signup");
        UserLogDTO secondDTO = new UserLogDTO(
                2L, LocalDateTime.now(), 1L, 5L, UserEventType.AUTHORIZATION_CHANGED,
                "Authorization changed: USER -> ADMIN");

        when(userLogRepository.findAll()).thenReturn(List.of(firstEntity, secondEntity));
        when(userLogMapper.toDTO(firstEntity)).thenReturn(firstDTO);
        when(userLogMapper.toDTO(secondEntity)).thenReturn(secondDTO);

        // Act
        List<UserLogDTO> result = userLogService.findAll();

        // Assert
        assertThat(result).containsExactly(firstDTO, secondDTO);
        verify(userLogRepository).findAll();
        verify(userLogMapper).toDTO(firstEntity);
        verify(userLogMapper).toDTO(secondEntity);
    }

    @Test
    @DisplayName("Checking if findAll returns an empty list when repository has no entries")
    void findAll_shouldReturnEmptyList_WhenRepositoryIsEmpty() {
        // Arrange
        when(userLogRepository.findAll()).thenReturn(List.of());

        // Act
        List<UserLogDTO> result = userLogService.findAll();

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(userLogMapper);
    }
}
