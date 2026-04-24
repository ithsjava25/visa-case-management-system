package org.example.visacasemanagementsystem.audit.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    // ── findFiltered ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findFiltered: delegates to repository with a non-null spec and maps the page content when all filters are null")
    @SuppressWarnings("unchecked")
    void findFiltered_shouldDelegateToRepo_andMapPage_WhenAllFiltersAreNull() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        UserLog entity = new UserLog();
        entity.setId(1L);
        UserLogDTO dto = new UserLogDTO(
                1L, LocalDateTime.now(), 1L, 1L, UserEventType.CREATED, "signup");

        when(userLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(userLogMapper.toDTO(entity)).thenReturn(dto);

        // Act
        Page<UserLogDTO> result = userLogService.findFiltered(null, null, null, pageable);

        // Assert
        assertThat(result.getContent()).containsExactly(dto);
        assertThat(result.getPageable()).isEqualTo(pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);

        ArgumentCaptor<Specification<UserLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(userLogRepository).findAll(specCaptor.capture(), eq(pageable));
        assertThat(specCaptor.getValue())
                .as("service must never pass a null Specification")
                .isNotNull();
    }

    @Test
    @DisplayName("findFiltered: returns an empty page and skips the mapper when the repository returns nothing")
    @SuppressWarnings("unchecked")
    void findFiltered_shouldReturnEmptyPage_WhenRepositoryHasNoMatches() {
        // Arrange
        Pageable pageable = PageRequest.of(1, 50);
        when(userLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        Page<UserLogDTO> result = userLogService.findFiltered(
                UserEventType.CREATED, null, null, pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verifyNoInteractions(userLogMapper);
    }

    @Test
    @DisplayName("findFiltered: builds an equality predicate on userEventType when only eventType is set")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findFiltered_shouldApplyEventTypeEqualityPredicate_WhenOnlyEventTypeIsSet() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(userLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        userLogService.findFiltered(UserEventType.AUTHORIZATION_CHANGED, null, null, pageable);

        // Assert - capture the composed Specification and execute it against mocks
        ArgumentCaptor<Specification<UserLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(userLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<UserLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get("userEventType")).thenReturn(path);
        when(cb.equal(path, UserEventType.AUTHORIZATION_CHANGED)).thenReturn(expectedPredicate);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(expectedPredicate);
        verify(root).get("userEventType");
        verify(cb).equal(path, UserEventType.AUTHORIZATION_CHANGED);
        verify(cb, never()).greaterThanOrEqualTo(any(Path.class), any(LocalDateTime.class));
        verify(cb, never()).lessThanOrEqualTo(any(Path.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("findFiltered: builds a greaterThanOrEqualTo predicate on timeStamp when only from is set")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findFiltered_shouldApplyFromPredicate_WhenOnlyFromIsSet() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        when(userLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        userLogService.findFiltered(null, from, null, pageable);

        // Assert
        ArgumentCaptor<Specification<UserLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(userLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<UserLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get("timeStamp")).thenReturn(path);
        when(cb.greaterThanOrEqualTo(path, from)).thenReturn(expectedPredicate);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(expectedPredicate);
        verify(root).get("timeStamp");
        verify(cb).greaterThanOrEqualTo(path, from);
        verify(cb, never()).equal(any(Path.class), any(UserEventType.class));
        verify(cb, never()).lessThanOrEqualTo(any(Path.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("findFiltered: builds a lessThanOrEqualTo predicate on timeStamp when only to is set")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findFiltered_shouldApplyToPredicate_WhenOnlyToIsSet() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime to = LocalDateTime.of(2026, 12, 31, 23, 59);
        when(userLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        userLogService.findFiltered(null, null, to, pageable);

        // Assert
        ArgumentCaptor<Specification<UserLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(userLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<UserLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get("timeStamp")).thenReturn(path);
        when(cb.lessThanOrEqualTo(path, to)).thenReturn(expectedPredicate);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(expectedPredicate);
        verify(root).get("timeStamp");
        verify(cb).lessThanOrEqualTo(path, to);
        verify(cb, never()).equal(any(Path.class), any(UserEventType.class));
        verify(cb, never()).greaterThanOrEqualTo(any(Path.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("findFiltered: composes event-type + from + to predicates with AND when all filters are supplied")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findFiltered_shouldComposeAllPredicates_WhenAllFiltersAreSet() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 12, 31, 23, 59);

        when(userLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        userLogService.findFiltered(UserEventType.CREATED, from, to, pageable);

        // Assert
        ArgumentCaptor<Specification<UserLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(userLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<UserLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path eventTypePath = mock(Path.class);
        Path timeStampPath = mock(Path.class);
        Predicate eqPred = mock(Predicate.class);
        Predicate gtePred = mock(Predicate.class);
        Predicate ltePred = mock(Predicate.class);
        Predicate and1 = mock(Predicate.class);
        Predicate and2 = mock(Predicate.class);

        when(root.get("userEventType")).thenReturn(eventTypePath);
        when(root.get("timeStamp")).thenReturn(timeStampPath);
        when(cb.equal(eventTypePath, UserEventType.CREATED)).thenReturn(eqPred);
        when(cb.greaterThanOrEqualTo(timeStampPath, from)).thenReturn(gtePred);
        when(cb.lessThanOrEqualTo(timeStampPath, to)).thenReturn(ltePred);
        when(cb.and(eqPred, gtePred)).thenReturn(and1);
        when(cb.and(and1, ltePred)).thenReturn(and2);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(and2);
        verify(cb).equal(eventTypePath, UserEventType.CREATED);
        verify(cb).greaterThanOrEqualTo(timeStampPath, from);
        verify(cb).lessThanOrEqualTo(timeStampPath, to);
    }
}
