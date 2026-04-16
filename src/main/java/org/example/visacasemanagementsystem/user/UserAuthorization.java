package org.example.visacasemanagementsystem.user;

public enum UserAuthorization {
    USER,
    ADMIN,
    SYSADMIN;

    @Override
    public String toString() {
        return "ROLE_" + this.name();
    }
}
