package org.example.visacasemanagementsystem.audit.service;

import org.example.visacasemanagementsystem.audit.CommentEventType;
import org.example.visacasemanagementsystem.audit.dto.CommentLogDTO;
import org.example.visacasemanagementsystem.audit.entity.CommentLog;
import org.example.visacasemanagementsystem.audit.mapper.CommentLogMapper;
import org.example.visacasemanagementsystem.audit.repository.CommentLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
}
