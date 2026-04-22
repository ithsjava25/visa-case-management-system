package org.example.visacasemanagementsystem.audit.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.visacasemanagementsystem.audit.VisaEventType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class VisaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visa_log_id", nullable = false)
    private Long id;

    @NotNull @CreatedDate
    private LocalDateTime timeStamp;

    @NotNull private Long userId; // Vem gjorde vad?

    @NotNull private Long visaCaseId; // Vilket ärende rör det?

    @NotNull @Enumerated(EnumType.STRING)
    private VisaEventType visaEventType;

    private String description; // Beskrivning av händelse

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VisaLog visaLog)) return false;
        return Objects.equals(id, visaLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "VisaLog{" +
                "id=" + id +
                ", timeStamp=" + timeStamp +
                ", userId=" + userId +
                ", visaCaseId=" + visaCaseId +
                ", visaEventType=" + visaEventType +
                ", description='" + description + '\'' +
                '}';
    }
}
