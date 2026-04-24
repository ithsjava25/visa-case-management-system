package org.example.visacasemanagementsystem.audit.mapper;

import org.example.visacasemanagementsystem.audit.CommentEventType;
import org.example.visacasemanagementsystem.audit.dto.CommentLogDTO;
import org.example.visacasemanagementsystem.audit.entity.CommentLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

@DisplayName("CommentLogMapper unit tests")
class CommentLogMapperTest {

    private CommentLogMapper commentLogMapper;

    @BeforeEach
  void setUp() {
        commentLogMapper = new CommentLogMapper();
    }

    @Test
    @DisplayName("Checking if toDTO copies every field from CommentLog entity to CommentLogDTO")
    void toDTO_shouldCopyAllFields_WhenEntityIsPopulated() {
        // Arrange
        LocalDateTime timestamp = LocalDateTime.of(2026, 4, 24, 14, 0,0);
        CommentLog entity =  new CommentLog();
        entity.setId(50L);
        entity.setTimeStamp(timestamp);
        entity.setActorUserId(1L);
        entity.setVisaCaseId(100L);
        entity.setCommentId(20L);
        entity.setCommentEventType(CommentEventType.ADDED);
        entity.setDescription("User added a comment regarding missing documents.");

        // Act
        CommentLogDTO dto = commentLogMapper.toDTO(entity);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(50L);
        assertThat(dto.timeStamp()).isEqualTo(timestamp);
        assertThat(dto.actorUserId()).isEqualTo(1L);
        assertThat(dto.visaCaseId()).isEqualTo(100L);
        assertThat(dto.commentId()).isEqualTo(20L);
        assertThat(dto.commentEventType()).isEqualTo(CommentEventType.ADDED);
        assertThat(dto.description()).isEqualTo("User added a comment regarding missing documents.");

    }

    @Test
    @DisplayName("Checking if toDTO returns null when entity is null")
    void toDTO_shouldReturnNull_WhenEntityIsNull() {
        // Act
        CommentLogDTO dto = commentLogMapper.toDTO(null);

        // Assert
        assertThat(dto).isNull();

    }

    @Test
    @DisplayName("Checking if toEntity sets all fields for a new log entry")
    void toEntity_shouldSetAllFields_WhenAllArgumentsAreProvided() {
        // Act
        CommentLog entity = commentLogMapper.toEntity(
                1L, 100L, 20L,
                CommentEventType.ADDED, "User added a comment"
        );

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getActorUserId()).isEqualTo(1L);
        assertThat(entity.getVisaCaseId()).isEqualTo(100L);
        assertThat(entity.getCommentId()).isEqualTo(20L);
        assertThat(entity.getCommentEventType()).isEqualTo(CommentEventType.ADDED);
        assertThat(entity.getDescription()).isEqualTo("User added a comment");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getTimeStamp()).isNull();
    }




}
