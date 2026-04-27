package org.example.visacasemanagementsystem.audit.service;

import org.example.visacasemanagementsystem.audit.FileEventType;
import org.example.visacasemanagementsystem.audit.dto.FileLogDTO;
import org.example.visacasemanagementsystem.audit.entity.FileLog;
import org.example.visacasemanagementsystem.audit.mapper.FileLogMapper;
import org.example.visacasemanagementsystem.audit.repository.FileLogRepository;
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
@DisplayName("FileLogService unit tests")
class FileLogServiceTest {

    @Mock
    private FileLogRepository fileLogRepository;
    @Mock
    private FileLogMapper fileLogMapper;

    @InjectMocks
    private FileLogService fileLogService;

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
}
