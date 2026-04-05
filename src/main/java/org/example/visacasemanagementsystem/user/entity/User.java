package org.example.visacasemanagementsystem.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.visacasemanagementsystem.user.UserAuthorization;

import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @NotBlank @Column(nullable = false, unique = true)
    private String username;

    @NotBlank @Column(nullable = false)
    private String fullName;

    @NotBlank @Column(unique = true)
    private String email;

    //Placeholder password storage solution
    @NotBlank private String password;

    @NotNull @Enumerated(EnumType.STRING)
    private UserAuthorization userAuthorization;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User user)) return false;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + userAuthorization +
                '}';
    }
}
