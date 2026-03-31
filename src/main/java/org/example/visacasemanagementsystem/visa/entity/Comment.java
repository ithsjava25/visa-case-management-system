package org.example.visacasemanagementsystem.visa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.visacasemanagementsystem.user.entity.User;

import java.time.LocalDateTime;
import java.util.Objects;


@Entity
@Getter
@Setter
@NoArgsConstructor
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

    private LocalDateTime createdAt = LocalDateTime.now();

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Comment comment)) return false;
        return Objects.equals(id, comment.id) && Objects.equals(visa, comment.visa) && Objects.equals(author, comment.author) && Objects.equals(text, comment.text) && Objects.equals(createdAt, comment.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, visa, author, text, createdAt);
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", visa=" + visa +
                ", author=" + author +
                ", text='" + text + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
