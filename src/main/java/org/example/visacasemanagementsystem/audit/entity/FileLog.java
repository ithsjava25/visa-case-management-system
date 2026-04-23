package org.example.visacasemanagementsystem.audit.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.visacasemanagementsystem.audit.FileEventType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FileLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_log_id", nullable = false)
    private Long id;

    @NotNull
    @CreatedDate
    private LocalDateTime timeStamp;

    @NotNull private Long actorUserId;

    @NotNull private Long visaCaseId;

    @NotNull private String fileName;

    @NotNull @Enumerated(EnumType.STRING)
    private FileEventType fileEventType;

    private String description;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FileLog fileLog)) return false;
        return Objects.equals(id, fileLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "FileLog{" +
                "id=" + id +
                ", timeStamp=" + timeStamp +
                ", actorUserId=" + actorUserId +
                ", visaCaseId=" + visaCaseId +
                ", fileName='" + fileName + '\'' +
                ", fileEventType=" + fileEventType +
                ", description='" + description + '\'' +
                '}';
    }
}
