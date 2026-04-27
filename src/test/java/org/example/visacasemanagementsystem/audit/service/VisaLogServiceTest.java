package org.example.visacasemanagementsystem.audit.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
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
import static org.mockito.ArgumentMatchers.nullable;
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

    // ── findFiltered ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findFiltered: delegates to repository with a non-null spec and maps the page content when all filters are null")
    @SuppressWarnings("unchecked")
    void findFiltered_shouldDelegateToRepo_andMapPage_WhenAllFiltersAreNull() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        VisaLog entity = new VisaLog();
        entity.setId(1L);
        VisaLogDTO dto = new VisaLogDTO(
                1L, LocalDateTime.now(), 1L, 10L, VisaEventType.CREATED, "x");

        when(visaLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(visaLogMapper.toDTO(entity)).thenReturn(dto);

        // Act
        Page<VisaLogDTO> result = visaLogService.findFiltered(null, null, null, pageable);

        // Assert
        assertThat(result.getContent()).containsExactly(dto);
        assertThat(result.getPageable()).isEqualTo(pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);

        ArgumentCaptor<Specification<VisaLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(visaLogRepository).findAll(specCaptor.capture(), eq(pageable));
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
        when(visaLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        Page<VisaLogDTO> result = visaLogService.findFiltered(
                VisaEventType.CREATED, null, null, pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verifyNoInteractions(visaLogMapper);
    }

    @Test
    @DisplayName("findFiltered: builds an equality predicate on visaEventType when only eventType is set")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findFiltered_shouldApplyEventTypeEqualityPredicate_WhenOnlyEventTypeIsSet() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(visaLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        visaLogService.findFiltered(VisaEventType.GRANTED, null, null, pageable);

        // Assert - capture the composed Specification and execute it against mocks
        ArgumentCaptor<Specification<VisaLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(visaLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<VisaLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(cb.equal(path, VisaEventType.GRANTED)).thenReturn(expectedPredicate);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(expectedPredicate);
        verify(cb).equal(path, VisaEventType.GRANTED);
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
        when(visaLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        visaLogService.findFiltered(null, from, null, pageable);

        // Assert
        ArgumentCaptor<Specification<VisaLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(visaLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<VisaLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(cb.greaterThanOrEqualTo(path, from)).thenReturn(expectedPredicate);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(expectedPredicate);
        verify(cb).greaterThanOrEqualTo(path, from);
        verify(cb, never()).equal(any(Path.class), any(VisaEventType.class));
        verify(cb, never()).lessThanOrEqualTo(any(Path.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("findFiltered: builds a lessThanOrEqualTo predicate on timeStamp when only to is set")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findFiltered_shouldApplyToPredicate_WhenOnlyToIsSet() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime to = LocalDateTime.of(2026, 12, 31, 23, 59);
        when(visaLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        visaLogService.findFiltered(null, null, to, pageable);

        // Assert
        ArgumentCaptor<Specification<VisaLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(visaLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<VisaLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(cb.lessThanOrEqualTo(path, to)).thenReturn(expectedPredicate);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(expectedPredicate);
        verify(cb).lessThanOrEqualTo(path, to);
        verify(cb, never()).equal(any(Path.class), any(VisaEventType.class));
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

        when(visaLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        visaLogService.findFiltered(VisaEventType.CREATED, from, to, pageable);

        // Assert
        ArgumentCaptor<Specification<VisaLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(visaLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<VisaLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path eventTypePath = mock(Path.class);
        Path timeStampPath = mock(Path.class);
        Predicate eqPred = mock(Predicate.class);
        Predicate gtePred = mock(Predicate.class);
        Predicate ltePred = mock(Predicate.class);
        Predicate and1 = mock(Predicate.class);
        Predicate and2 = mock(Predicate.class);

        // The service's specification builder evaluates the three filters in a
        // fixed order (event-type → from → to), and chains the resulting
        // predicates with cb.and((eventType AND from), to). The multi-value
        // thenReturn stubs below mirror that exact sequence — they would need
        // updating if the order in VisaLogService#findFiltered ever changes.
        when(root.get(nullable(SingularAttribute.class))).thenReturn(eventTypePath, timeStampPath, timeStampPath);
        when(cb.equal(eventTypePath, VisaEventType.CREATED)).thenReturn(eqPred);
        when(cb.greaterThanOrEqualTo(timeStampPath, from)).thenReturn(gtePred);
        when(cb.lessThanOrEqualTo(timeStampPath, to)).thenReturn(ltePred);
        when(cb.and(eqPred, gtePred)).thenReturn(and1);
        when(cb.and(and1, ltePred)).thenReturn(and2);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(and2);
        verify(cb).equal(eventTypePath, VisaEventType.CREATED);
        verify(cb).greaterThanOrEqualTo(timeStampPath, from);
        verify(cb).lessThanOrEqualTo(timeStampPath, to);
    }
}
