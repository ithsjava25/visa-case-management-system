package org.example.visacasemanagementsystem.audit.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import org.example.visacasemanagementsystem.audit.CommentEventType;
import org.example.visacasemanagementsystem.audit.dto.CommentLogDTO;
import org.example.visacasemanagementsystem.audit.entity.CommentLog;
import org.example.visacasemanagementsystem.audit.mapper.CommentLogMapper;
import org.example.visacasemanagementsystem.audit.repository.CommentLogRepository;
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
@DisplayName("CommentLogService unit tests")
class CommentLogServiceTest {

    @Mock
    private CommentLogRepository commentLogRepository;
    @Mock
    private CommentLogMapper commentLogMapper;

    @InjectMocks
    private CommentLogService commentLogService;

    // ── createCommentLog ──────────────────────────────────────────────────────

    @Test
    @DisplayName("createCommentLog should map to entity and save to repository")
    void createComment_shouldMapAndSaveLog() {
        // Arrange
        Long actorId = 1L;
        Long visaId = 100L;
        Long commentId = 20L;
        CommentEventType type = CommentEventType.ADDED;
        String description = "Test description";

        CommentLog mockEntity = new CommentLog();

        when(commentLogMapper.toEntity(actorId, visaId, commentId, type, description)).thenReturn(mockEntity);

        // Act
        commentLogService.createCommentLog(actorId, visaId, commentId, type, description);

        // Assert
        verify(commentLogMapper, times(1)).toEntity(actorId, visaId, commentId, type, description);
        verify(commentLogRepository, times(1)).save(mockEntity);
    }

    @Test
    @DisplayName("Checking if createCommentLog passes the exact same entity instance from mapper to repository")
    void createCommentLog_shouldPassSameEntityInstance_WhenSavingToRepository() {
        // Arrange
        CommentLog mappedEntity = new CommentLog();
        when(commentLogMapper.toEntity(any(), any(), any(), any(), any()))
                .thenReturn(mappedEntity);

        // Act
        commentLogService.createCommentLog(1L, 1L, 1L, CommentEventType.ADDED, "x");

        // Assert
        ArgumentCaptor<CommentLog> captor = ArgumentCaptor.forClass(CommentLog.class);
        verify(commentLogRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(mappedEntity);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll should return a list of DTOs")
    void findAll_shouldReturnDtoToList() {
        // Arrange
        CommentLog log1  = new CommentLog();
        CommentLog log2  = new CommentLog();
        when(commentLogRepository.findAll()).thenReturn(List.of(log1, log2));

        CommentLogDTO mockDto = mock(CommentLogDTO.class);
        when(commentLogMapper.toDTO(any(CommentLog.class))).thenReturn(mockDto);
        // act
        var result =  commentLogService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        verify(commentLogRepository, times(1)).findAll();
        verify(commentLogMapper, times(2)).toDTO(any(CommentLog.class));
    }

    @Test
    @DisplayName("Checking if findAll returns an empty list when repository has no entries")
    void findAll_shouldReturnEmptyList_WhenRepositoryIsEmpty() {
        // Arrange
        when(commentLogRepository.findAll()).thenReturn(List.of());

        // Act
        List<CommentLogDTO> result = commentLogService.findAll();

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(commentLogMapper);
    }

    // ── findFiltered ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findFiltered: delegates to repository with a non-null spec and maps the page content when all filters are null")
    @SuppressWarnings("unchecked")
    void findFiltered_shouldDelegateToRepo_andMapPage_WhenAllFiltersAreNull() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        CommentLog entity = new CommentLog();
        entity.setId(1L);
        CommentLogDTO dto = new CommentLogDTO(
                1L, LocalDateTime.now(), 1L, 10L, 7L, CommentEventType.ADDED, "x");

        when(commentLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(commentLogMapper.toDTO(entity)).thenReturn(dto);

        // Act
        Page<CommentLogDTO> result = commentLogService.findFiltered(null, null, null, pageable);

        // Assert
        assertThat(result.getContent()).containsExactly(dto);
        assertThat(result.getPageable()).isEqualTo(pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);

        ArgumentCaptor<Specification<CommentLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(commentLogRepository).findAll(specCaptor.capture(), eq(pageable));
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
        when(commentLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        Page<CommentLogDTO> result = commentLogService.findFiltered(
                CommentEventType.ADDED, null, null, pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verifyNoInteractions(commentLogMapper);
    }

    @Test
    @DisplayName("findFiltered: builds an equality predicate on commentEventType when only eventType is set")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findFiltered_shouldApplyEventTypeEqualityPredicate_WhenOnlyEventTypeIsSet() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(commentLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        commentLogService.findFiltered(CommentEventType.ADDED, null, null, pageable);

        // Assert - capture the composed Specification and execute it against mocks
        ArgumentCaptor<Specification<CommentLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(commentLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<CommentLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(cb.equal(path, CommentEventType.ADDED)).thenReturn(expectedPredicate);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(expectedPredicate);
        verify(cb).equal(path, CommentEventType.ADDED);
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
        when(commentLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        commentLogService.findFiltered(null, from, null, pageable);

        // Assert
        ArgumentCaptor<Specification<CommentLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(commentLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<CommentLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(cb.greaterThanOrEqualTo(path, from)).thenReturn(expectedPredicate);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(expectedPredicate);
        verify(cb).greaterThanOrEqualTo(path, from);
        verify(cb, never()).equal(any(Path.class), any(CommentEventType.class));
        verify(cb, never()).lessThanOrEqualTo(any(Path.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("findFiltered: builds a lessThanOrEqualTo predicate on timeStamp when only to is set")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findFiltered_shouldApplyToPredicate_WhenOnlyToIsSet() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime to = LocalDateTime.of(2026, 12, 31, 23, 59);
        when(commentLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        commentLogService.findFiltered(null, null, to, pageable);

        // Assert
        ArgumentCaptor<Specification<CommentLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(commentLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<CommentLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(cb.lessThanOrEqualTo(path, to)).thenReturn(expectedPredicate);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(expectedPredicate);
        verify(cb).lessThanOrEqualTo(path, to);
        verify(cb, never()).equal(any(Path.class), any(CommentEventType.class));
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

        when(commentLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        commentLogService.findFiltered(CommentEventType.UPDATED, from, to, pageable);

        // Assert
        ArgumentCaptor<Specification<CommentLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(commentLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<CommentLog> root = mock(Root.class);
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
        // updating if the order in CommentLogService#findFiltered ever changes.
        when(root.get(nullable(SingularAttribute.class))).thenReturn(eventTypePath, timeStampPath, timeStampPath);
        when(cb.equal(eventTypePath, CommentEventType.UPDATED)).thenReturn(eqPred);
        when(cb.greaterThanOrEqualTo(timeStampPath, from)).thenReturn(gtePred);
        when(cb.lessThanOrEqualTo(timeStampPath, to)).thenReturn(ltePred);
        when(cb.and(eqPred, gtePred)).thenReturn(and1);
        when(cb.and(and1, ltePred)).thenReturn(and2);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(and2);
        verify(cb).equal(eventTypePath, CommentEventType.UPDATED);
        verify(cb).greaterThanOrEqualTo(timeStampPath, from);
        verify(cb).lessThanOrEqualTo(timeStampPath, to);
    }
}
