package org.example.visacasemanagementsystem.audit.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.visacasemanagementsystem.audit.AuditEventType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id", nullable = false)
    private Long id;

    @NotNull
    @CreatedDate
    private LocalDateTime timeStamp;

    @NotNull
    private Long userId; // Vem gjorde vad?

    @NotNull
    private Long visaCaseId; // Vilket ärende rör det?

    @NotNull
    @Enumerated(EnumType.STRING)
    private AuditEventType auditEventType;

    private String description; // Beskrivning av händelse

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuditLog auditLog)) return false;
        return Objects.equals(id, auditLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", timeStamp=" + timeStamp +
                ", userId=" + userId +
                ", visaCaseId=" + visaCaseId +
                ", auditEventType=" + auditEventType +
                ", description='" + description + '\'' +
                '}';
    }
}
