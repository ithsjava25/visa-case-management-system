package org.example.visacasemanagementsystem.visa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.visa.VisaStatus;
import org.example.visacasemanagementsystem.visa.VisaType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Visa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visa_id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private VisaType visaType;

    @NotNull
    @Enumerated(EnumType.STRING)
    private VisaStatus visaStatus;

    @NotBlank
    private String nationality;

    @ManyToOne
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant; // Den som söker visumet

    @ManyToOne
    @JoinColumn(name = "handler_id")
    private User handler; // Handläggaren (Admin) --> Används vid tilldelning/filtrering & validering

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private String rejectionReason;

    @ElementCollection
    @CollectionTable(name = "visa_documents", joinColumns = @JoinColumn(name = "visa_id"))
    @Column(name = "s3_key")
    private List<String> s3Keys = new ArrayList<>();

    //ToDo: Comments in the form of a discussion between applicant and administrator.


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Visa visa)) return false;
        return Objects.equals(id, visa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Visa{" +
                "id=" + id +
                ", visaType=" + visaType +
                ", visaStatus=" + visaStatus +
                ", nationality='" + nationality + '\'' +
                ", applicantId=" + (applicant != null ? applicant.getId() : "null") +
                ", handlerId=" + (handler != null ? handler.getId() : "null") +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", rejectionReason='" + rejectionReason + '\'' +
                ", s3Keys=" + s3Keys +
                '}';
    }
}
