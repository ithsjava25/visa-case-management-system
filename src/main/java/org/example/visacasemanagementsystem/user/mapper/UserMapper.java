package org.example.visacasemanagementsystem.user.mapper;

import org.example.visacasemanagementsystem.user.dto.CreateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UpdateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    // För visning (Entity --> DTO)
    public UserDTO toDTO(User user){
        if (user == null) return null;

        return new UserDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getUserAuthorization()
        );
    }

    // CreateDTO --> Entity
    public User toEntity(CreateUserDTO dto){
        if (dto == null) return null;

        User user = new User();
        user.setFullName(dto.fullName());
        user.setEmail(dto.email());
        user.setUsername(dto.email());
        user.setPassword(dto.password());
        user.setUserAuthorization(dto.userAuthorization());
        return user;
    }

    // Uppdatering (DTO --> Befintlig Entity)
    public void updateEntityFromDTO(UpdateUserDTO dto, User user) {
        if (dto == null || user == null) return;

        user.setFullName(dto.fullName());
        user.setEmail(dto.email());
        user.setUsername(dto.email());
    }
}
