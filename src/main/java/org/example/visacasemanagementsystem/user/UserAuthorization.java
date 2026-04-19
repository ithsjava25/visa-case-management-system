package org.example.visacasemanagementsystem.user;

public enum UserAuthorization {
    USER,
    ADMIN,
    SYSADMIN;

    public String asAuthority() {
        return "ROLE_" + name();
    }
}
