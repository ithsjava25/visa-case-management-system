package org.example.visacasemanagementsystem.visa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.visa.VisaStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Visa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visa_id", nullable = false, updatable = false)
    private Long id;

    @NotBlank
    private String visaType;

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
    private User handler; // Handläggaren (Admin) som tilldelar ärendet

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    @ElementCollection
    @CollectionTable(name = "visa_documents", joinColumns = @JoinColumn(name = "visa_id"))
    @Column(name = "s3_key")
    private List<String> s3Keys = new ArrayList<>();

    //ToDo: Comments in the form of a discussion between applicant and administrator.


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Visa visa)) return false;
        return Objects.equals(id, visa.id) && Objects.equals(visaType, visa.visaType) && visaStatus == visa.visaStatus && Objects.equals(nationality, visa.nationality) && Objects.equals(applicant, visa.applicant) && Objects.equals(handler, visa.handler) && Objects.equals(createdAt, visa.createdAt) && Objects.equals(updatedAt, visa.updatedAt) && Objects.equals(s3Keys, visa.s3Keys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, visaType, visaStatus, nationality, applicant, handler, createdAt, updatedAt, s3Keys);
    }

    @Override
    public String toString() {
        return "Visa{" +
                "id=" + id +
                ", visaType='" + visaType + '\'' +
                ", visaStatus=" + visaStatus +
                ", nationality='" + nationality + '\'' +
                ", applicant=" + applicant +
                ", handler=" + handler +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", s3Keys=" + s3Keys +
                '}';
    }
}
