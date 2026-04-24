package org.example.visacasemanagementsystem.audit.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.visacasemanagementsystem.audit.CommentEventType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CommentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_log_id", nullable = false)
    private Long id;

    @NotNull @CreatedDate
    private LocalDateTime timeStamp;

    @NotNull private Long actorUserId;

    @NotNull private Long visaCaseId;

    @NotNull private Long commentId;

    @NotNull @Enumerated(EnumType.STRING)
    private CommentEventType commentEventType;

    private String description;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CommentLog that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "CommentLog{" +
                "id=" + id +
                ", timeStamp=" + timeStamp +
                ", actorUserId=" + actorUserId +
                ", visaCaseId=" + visaCaseId +
                ", commentId=" + commentId +
                ", commentEventType=" + commentEventType +
                ", description='" + description + '\'' +
                '}';
    }
}
