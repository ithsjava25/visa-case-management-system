package org.example.visacasemanagementsystem.comment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;


@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "visa_id", nullable = false)
    private Visa visa; // Vilket ärende tillhör kommentaren?

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String text;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Comment comment)) return false;
        return Objects.equals(id, comment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", visaId=" + (visa != null ? visa.getId() : null) +
                ", authorId=" + (author != null ? author.getId() : null) +
                ", text='" + text + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
