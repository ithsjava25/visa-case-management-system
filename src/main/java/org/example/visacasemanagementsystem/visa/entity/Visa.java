package org.example.visacasemanagementsystem.visa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.visacasemanagementsystem.visa.VisaStatus;

@Entity
public class Visa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @NotBlank
    private String visa;

    @NotNull
    @Enumerated(EnumType.STRING)
    private VisaStatus visaStatus;

    @NotBlank
    private String nationality;

    //ToDo: Comments in the form of a discussion between applicant and administrator.
}
