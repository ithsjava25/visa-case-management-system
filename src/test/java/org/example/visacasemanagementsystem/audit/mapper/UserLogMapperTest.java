package org.example.visacasemanagementsystem.audit.mapper;

import org.example.visacasemanagementsystem.audit.UserEventType;
import org.example.visacasemanagementsystem.audit.dto.UserLogDTO;
import org.example.visacasemanagementsystem.audit.entity.UserLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserLogMapper unit tests")
class UserLogMapperTest {

    private UserLogMapper userLogMapper;

    @BeforeEach
    void setUp() {
        userLogMapper = new UserLogMapper();
    }

    // ── toDTO ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if toDTO copies every field from UserLog entity to UserLogDTO")
    void toDTO_shouldCopyAllFields_WhenEntityIsPopulated() {
        // Arrange
        LocalDateTime timestamp = LocalDateTime.of(2026, 4, 22, 10, 30, 0);

        UserLog entity = new UserLog();
        entity.setId(11L);
        entity.setTimeStamp(timestamp);
        entity.setActorUserId(1L);   // sysadmin
        entity.setTargetUserId(5L);  // user being promoted
        entity.setUserEventType(UserEventType.AUTHORIZATION_CHANGED);
        entity.setDescription("Authorization changed: USER -> ADMIN");

        // Act
        UserLogDTO dto = userLogMapper.toDTO(entity);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(11L);
        assertThat(dto.timeStamp()).isEqualTo(timestamp);
        assertThat(dto.actorUserId()).isEqualTo(1L);
        assertThat(dto.targetUserId()).isEqualTo(5L);
        assertThat(dto.userEventType()).isEqualTo(UserEventType.AUTHORIZATION_CHANGED);
        assertThat(dto.description()).isEqualTo("Authorization changed: USER -> ADMIN");
    }

    @Test
    @DisplayName("Checking if toDTO returns null when entity is null")
    void toDTO_shouldReturnNull_WhenEntityIsNull() {
        // Act
        UserLogDTO dto = userLogMapper.toDTO(null);

        // Assert
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("Checking if toDTO keeps actor and target distinct (sysadmin acting on a different user)")
    void toDTO_shouldKeepActorAndTargetDistinct_WhenActorIsNotTarget() {
        // Arrange
        UserLog entity = new UserLog();
        entity.setActorUserId(1L);
        entity.setTargetUserId(99L);
        entity.setUserEventType(UserEventType.DELETED);
        entity.setDescription("User account deleted.");

        // Act
        UserLogDTO dto = userLogMapper.toDTO(entity);

        // Assert
        assertThat(dto.actorUserId()).isEqualTo(1L);
        assertThat(dto.targetUserId()).isEqualTo(99L);
        assertThat(dto.actorUserId()).isNotEqualTo(dto.targetUserId());
    }

    @Test
    @DisplayName("Checking if toDTO keeps actor and target equal (self-action like signup)")
    void toDTO_shouldKeepActorAndTargetEqual_WhenSelfAction() {
        // Arrange
        UserLog entity = new UserLog();
        entity.setActorUserId(7L);
        entity.setTargetUserId(7L);
        entity.setUserEventType(UserEventType.CREATED);
        entity.setDescription("User account created via signup.");

        // Act
        UserLogDTO dto = userLogMapper.toDTO(entity);

        // Assert
        assertThat(dto.actorUserId()).isEqualTo(dto.targetUserId());
    }

    // ── toEntity ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if toEntity sets all four fields and leaves id and timeStamp for JPA to fill in")
    void toEntity_shouldSetAllFields_WhenAllArgumentsAreProvided() {
        // Act
        UserLog entity = userLogMapper.toEntity(
                1L,
                5L,
                UserEventType.AUTHORIZATION_CHANGED,
                "Authorization changed: USER -> ADMIN"
        );

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getActorUserId()).isEqualTo(1L);
        assertThat(entity.getTargetUserId()).isEqualTo(5L);
        assertThat(entity.getUserEventType()).isEqualTo(UserEventType.AUTHORIZATION_CHANGED);
        assertThat(entity.getDescription()).isEqualTo("Authorization changed: USER -> ADMIN");
        // id and timeStamp are populated by JPA / @CreatedDate, not by the mapper
        assertThat(entity.getId()).isNull();
        assertThat(entity.getTimeStamp()).isNull();
    }

    @Test
    @DisplayName("Checking if toEntity supports actor == target for self-actions (signup, self-edit)")
    void toEntity_shouldAllowActorEqualToTarget_WhenSelfAction() {
        // Act
        UserLog entity = userLogMapper.toEntity(
                7L, 7L, UserEventType.CREATED, "User account created via signup."
        );

        // Assert
        assertThat(entity.getActorUserId()).isEqualTo(entity.getTargetUserId());
        assertThat(entity.getUserEventType()).isEqualTo(UserEventType.CREATED);
    }
}
