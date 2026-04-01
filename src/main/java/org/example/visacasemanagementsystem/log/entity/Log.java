package org.example.visacasemanagementsystem.log.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.example.visacasemanagementsystem.log.LogEvent;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @NotNull
    private LocalDateTime timeStamp;

    @NotNull
    private Long userId; // Vem gjorde vad?

    @NotNull
    private Long visaCaseId; // Vilket ärende rör det?

    @NotNull
    @Enumerated(EnumType.STRING)
    private LogEvent logEvent;

    private String description; // Beskrivning av händelse

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Log log)) return false;
        return Objects.equals(id, log.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Log{" +
                "id=" + id +
                ", timeStamp=" + timeStamp +
                ", userId=" + userId +
                ", visaCaseId=" + visaCaseId +
                ", logEvent=" + logEvent +
                ", description='" + description + '\'' +
                '}';
    }
}
