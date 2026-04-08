package org.example.visacasemanagementsystem.user.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.dto.CreateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UpdateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.mapper.UserMapper;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private static final String USER_NOT_FOUND = "User not found";

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public List<UserDTO> findAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDTO)
                .toList();
    }

    public Optional<UserDTO> findById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO);
    }

    public Optional<UserDTO> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDTO);
    }

    @Transactional
    public UserDTO createUser(CreateUserDTO dto) {
        User user = userMapper.toEntity(dto);
        user.setPassword(dto.password()); // mapper doesn't set this currently
        try {
            User savedUser = userRepository.save(user);
            return userMapper.toDTO(savedUser);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("A user with this email already exists", e);
        }
    }

    @Transactional
    public UserDTO updateUser(UpdateUserDTO dto) {
        // Check if user and email exists
        User user = userRepository.findById(dto.id())
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        userRepository.findByEmail(dto.email())
                .filter(existing -> !existing.getId().equals(dto.id()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A user with this email already exists");
                });

        userMapper.updateEntityFromDTO(dto, user);
        User savedUser = userRepository.save(user);
        return userMapper.toDTO(savedUser);
    }

    @Transactional
    public UserDTO updateUserAuthorization(Long userId, UserAuthorization newAuth, Long requesterId) {
        validateSysAdmin(requesterId); // confirm requester is SYSADMIN

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        user.setUserAuthorization(newAuth);
        User savedUser = userRepository.save(user);
        return userMapper.toDTO(savedUser);
    }

    @Transactional
    public void deleteUser(Long id, Long requesterId) {
        validateSysAdmin(requesterId); // confirm requester is SYSADMIN
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
        userRepository.delete(user);
    }

    private void validateSysAdmin(Long requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        if (requester.getUserAuthorization() != UserAuthorization.SYSADMIN) {
            throw new UnauthorizedException("User is not authorized to perform this action.");
        }
    }
}
