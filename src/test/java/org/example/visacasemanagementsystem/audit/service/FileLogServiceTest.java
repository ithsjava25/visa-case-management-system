package org.example.visacasemanagementsystem.audit.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import org.example.visacasemanagementsystem.audit.FileEventType;
import org.example.visacasemanagementsystem.audit.dto.FileLogDTO;
import org.example.visacasemanagementsystem.audit.entity.FileLog;
import org.example.visacasemanagementsystem.audit.mapper.FileLogMapper;
import org.example.visacasemanagementsystem.audit.repository.FileLogRepository;
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
@DisplayName("FileLogService unit tests")
class FileLogServiceTest {

    @Mock
    private FileLogRepository fileLogRepository;
    @Mock
    private FileLogMapper fileLogMapper;

    @InjectMocks
    private FileLogService fileLogService;

    // ── createFileLog ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createFileLog should map to entity and save to repository")
    void createFileLog_shouldMapAndSave() {
        // Arrange
        Long actorId = 7L;
        Long visaId = 200L;
        String fileName = "passport.pdf";
        FileEventType type = FileEventType.UPLOADED;
        String description = "File uploaded test";

        FileLog mockEntity = new FileLog();

        when(fileLogMapper.toEntity(actorId,visaId, fileName,type,description))
                .thenReturn(mockEntity);

        // Act
        fileLogService.createFileLog(actorId,visaId,fileName,type,description);

        // Assert
        verify(fileLogMapper, times(1))
                .toEntity(actorId,visaId,fileName,type,description);
        verify(fileLogRepository, times(1)).save(mockEntity);

    }

    @Test
    @DisplayName("Checking if createFileLog passes the exact same entity instance from mapper to repository")
    void createFileLog_shouldPassSameEntityInstance_WhenSavingToRepository() {
        // Arrange
        FileLog mappedEntity = new FileLog();
        when(fileLogMapper.toEntity(any(), any(), any(), any(), any()))
                .thenReturn(mappedEntity);

        // Act
        fileLogService.createFileLog(1L, 1L, "doc.pdf", FileEventType.UPLOADED, "x");

        // Assert
        ArgumentCaptor<FileLog> captor = ArgumentCaptor.forClass(FileLog.class);
        verify(fileLogRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(mappedEntity);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll should return a list of FileLogDTOs")
    void findAll_shouldReturnDtoList() {
        // Arrange
        FileLog log1 =  new FileLog();
        FileLog log2 =  new FileLog();
        when(fileLogRepository.findAll()).thenReturn(List.of(log1,log2));

        FileLogDTO mockDto = mock(FileLogDTO.class);
        when(fileLogMapper.toDTO(any(FileLog.class))).thenReturn(mockDto);

        // Act
        List<FileLogDTO> result = fileLogService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        verify(fileLogRepository, times(1)).findAll();
        verify(fileLogMapper, times(2)).toDTO(any(FileLog.class));
    }

    @Test
    @DisplayName("Checking if findAll returns an empty list when repository has no entries")
    void findAll_shouldReturnEmptyList_WhenRepositoryIsEmpty() {
        // Arrange
        when(fileLogRepository.findAll()).thenReturn(List.of());

        // Act
        List<FileLogDTO> result = fileLogService.findAll();

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(fileLogMapper);
    }

    // ── findFiltered ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findFiltered: delegates to repository with a non-null spec and maps the page content when all filters are null")
    @SuppressWarnings("unchecked")
    void findFiltered_shouldDelegateToRepo_andMapPage_WhenAllFiltersAreNull() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        FileLog entity = new FileLog();
        entity.setId(1L);
        FileLogDTO dto = new FileLogDTO(
                1L, LocalDateTime.now(), 1L, 10L, "passport.pdf", FileEventType.UPLOADED, "x");

        when(fileLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(fileLogMapper.toDTO(entity)).thenReturn(dto);

        // Act
        Page<FileLogDTO> result = fileLogService.findFiltered(null, null, null, pageable);

        // Assert
        assertThat(result.getContent()).containsExactly(dto);
        assertThat(result.getPageable()).isEqualTo(pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);

        ArgumentCaptor<Specification<FileLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(fileLogRepository).findAll(specCaptor.capture(), eq(pageable));
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
        when(fileLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        Page<FileLogDTO> result = fileLogService.findFiltered(
                FileEventType.UPLOADED, null, null, pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verifyNoInteractions(fileLogMapper);
    }

    @Test
    @DisplayName("findFiltered: builds an equality predicate on fileEventType when only eventType is set")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findFiltered_shouldApplyEventTypeEqualityPredicate_WhenOnlyEventTypeIsSet() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(fileLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        fileLogService.findFiltered(FileEventType.UPLOADED, null, null, pageable);

        // Assert - capture the composed Specification and execute it against mocks
        ArgumentCaptor<Specification<FileLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(fileLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<FileLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(cb.equal(path, FileEventType.UPLOADED)).thenReturn(expectedPredicate);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(expectedPredicate);
        verify(cb).equal(path, FileEventType.UPLOADED);
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
        when(fileLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        fileLogService.findFiltered(null, from, null, pageable);

        // Assert
        ArgumentCaptor<Specification<FileLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(fileLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<FileLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(cb.greaterThanOrEqualTo(path, from)).thenReturn(expectedPredicate);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(expectedPredicate);
        verify(cb).greaterThanOrEqualTo(path, from);
        verify(cb, never()).equal(any(Path.class), any(FileEventType.class));
        verify(cb, never()).lessThanOrEqualTo(any(Path.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("findFiltered: builds a lessThanOrEqualTo predicate on timeStamp when only to is set")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findFiltered_shouldApplyToPredicate_WhenOnlyToIsSet() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime to = LocalDateTime.of(2026, 12, 31, 23, 59);
        when(fileLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        fileLogService.findFiltered(null, null, to, pageable);

        // Assert
        ArgumentCaptor<Specification<FileLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(fileLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<FileLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(cb.lessThanOrEqualTo(path, to)).thenReturn(expectedPredicate);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(expectedPredicate);
        verify(cb).lessThanOrEqualTo(path, to);
        verify(cb, never()).equal(any(Path.class), any(FileEventType.class));
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

        when(fileLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        fileLogService.findFiltered(FileEventType.DOWNLOADED, from, to, pageable);

        // Assert
        ArgumentCaptor<Specification<FileLog>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(fileLogRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<FileLog> root = mock(Root.class);
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
        // updating if the order in FileLogService#findFiltered ever changes.
        when(root.get(nullable(SingularAttribute.class))).thenReturn(eventTypePath, timeStampPath, timeStampPath);
        when(cb.equal(eventTypePath, FileEventType.DOWNLOADED)).thenReturn(eqPred);
        when(cb.greaterThanOrEqualTo(timeStampPath, from)).thenReturn(gtePred);
        when(cb.lessThanOrEqualTo(timeStampPath, to)).thenReturn(ltePred);
        when(cb.and(eqPred, gtePred)).thenReturn(and1);
        when(cb.and(and1, ltePred)).thenReturn(and2);

        Predicate actual = specCaptor.getValue().toPredicate(root, query, cb);

        assertThat(actual).isSameAs(and2);
        verify(cb).equal(eventTypePath, FileEventType.DOWNLOADED);
        verify(cb).greaterThanOrEqualTo(timeStampPath, from);
        verify(cb).lessThanOrEqualTo(timeStampPath, to);
    }
}
