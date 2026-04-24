package org.example.visacasemanagementsystem.audit.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.visacasemanagementsystem.audit.UserEventType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_log_id", nullable = false)
    private Long id;

    @NotNull @CreatedDate
    private LocalDateTime timeStamp;

    // Who performed the action (e.g. the sysadmin who deleted the account, or the
    // user themselves when self-creating via signup or self-updating their profile).
    @NotNull private Long actorUserId;

    // Which user the action was performed on. For self-creation/self-update this
    // equals actorUserId; for admin-initiated changes it differs.
    @NotNull private Long targetUserId;

    @NotNull @Enumerated(EnumType.STRING)
    private UserEventType userEventType;

    private String description; // Free-form description of what happened.

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UserLog userLog)) return false;
        return Objects.equals(id, userLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "UserLog{" +
                "id=" + id +
                ", timeStamp=" + timeStamp +
                ", actorUserId=" + actorUserId +
                ", targetUserId=" + targetUserId +
                ", userEventType=" + userEventType +
                ", description='" + description + '\'' +
                '}';
    }
}
