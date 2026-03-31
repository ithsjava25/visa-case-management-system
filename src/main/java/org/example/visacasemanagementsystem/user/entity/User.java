package org.example.visacasemanagementsystem.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.visacasemanagementsystem.user.UserAuthorization;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @NotBlank
    private String username;

    @NotBlank
    private String fullName;

    @NotBlank
    private String email;

    //Placeholder password storage solution
    @NotBlank
    private String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    private UserAuthorization userAuthorization;
}
