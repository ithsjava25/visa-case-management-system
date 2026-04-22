package org.example.visacasemanagementsystem.user.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.visacasemanagementsystem.audit.UserEventType;
import org.example.visacasemanagementsystem.audit.service.UserLogService;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.dto.CreateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UpdateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.mapper.UserMapper;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService unit tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserLogService userLogService;

    @InjectMocks
    private UserService userService;

    // ── createUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if createUser throws IllegalArgumentException when password is shorter than 8 characters")
    void createUser_shouldThrowIllegalArgumentException_WhenPasswordIsTooShort() {
        // Arrange
        CreateUserDTO dto = new CreateUserDTO(
                "Short Pass", "short@test.com", "abc", UserAuthorization.USER
        );

        when(userMapper.toEntity(dto)).thenReturn(new User());

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 characters");

        // Verify — repository should never be called
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Checking if createUser throws IllegalArgumentException when email is already taken")
    void createUser_shouldThrowIllegalArgumentException_WhenEmailAlreadyExists() {
        // Arrange
        CreateUserDTO dto = new CreateUserDTO(
                "Duplicate", "existing@test.com", "password123", UserAuthorization.USER
        );

        User mappedUser = new User();
        when(userMapper.toEntity(dto)).thenReturn(mappedUser);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email already exists");
    }

    @Test
    @DisplayName("Checking if createUser persists and returns a UserDTO when input is valid")
    void createUser_shouldSaveAndReturnUserDTO_WhenDataIsValid() {
        // Arrange
        CreateUserDTO dto = new CreateUserDTO(
                "New User", "new@test.com", "password123", UserAuthorization.SYSADMIN
        );

        User mappedUser = new User();
        User savedUser = new User();
        savedUser.setId(1L);
        UserDTO expectedDTO = new UserDTO(1L, "New User", "new@test.com", UserAuthorization.USER);

        when(userMapper.toEntity(dto)).thenReturn(mappedUser);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(userMapper.toDTO(savedUser)).thenReturn(expectedDTO);

        // Act
        UserDTO result = userService.createUser(dto);

        // Assert
        assertThat(result).isEqualTo(expectedDTO);
        assertThat(mappedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(mappedUser.getUserAuthorization()).isEqualTo(UserAuthorization.USER);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(mappedUser);
        // Self-creation via signup: actor and target are the same (the new user).
        verify(userLogService).createUserLog(eq(1L), eq(1L), eq(UserEventType.CREATED), anyString());
    }

    // ── updateUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if updateUser throws EntityNotFoundException when user ID does not exist")
    void updateUser_shouldThrowEntityNotFoundException_WhenUserDoesNotExist() {
        // Arrange
        UpdateUserDTO dto = new UpdateUserDTO(999L, "Name", "email@test.com");
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(dto, 999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("Checking if updateUser throws IllegalArgumentException when new email is already taken by another user")
    void updateUser_shouldThrowIllegalArgumentException_WhenEmailAlreadyExists() {
        // Arrange
        Long userId = 1L;
        UpdateUserDTO dto = new UpdateUserDTO(userId, "User", "taken@test.com");

        User existingUser = new User();
        existingUser.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(dto, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email already exists");
    }

    @Test
    @DisplayName("Checking if updateUser updates fields and returns the updated UserDTO")
    void updateUser_shouldUpdateAndReturnUser_WhenDataIsValid() {
        // Arrange
        Long userId = 1L;
        UpdateUserDTO dto = new UpdateUserDTO(userId, "Updated Name", "updated@test.com");

        User existingUser = new User();
        existingUser.setId(userId);
        UserDTO expectedDTO = new UserDTO(userId, "Updated Name", "updated@test.com", UserAuthorization.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(existingUser);
        when(userMapper.toDTO(existingUser)).thenReturn(expectedDTO);

        // Act
        UserDTO result = userService.updateUser(dto, userId);

        // Assert
        assertThat(result).isEqualTo(expectedDTO);
        verify(userMapper).updateEntityFromDTO(dto, existingUser);
        verify(userRepository).saveAndFlush(existingUser);
        verify(userLogService).createUserLog(eq(userId), eq(userId), eq(UserEventType.UPDATED), anyString());
    }

    // ── updateUserAuthorization ───────────────────────────────────────────────

    @Test
    @DisplayName("Checking if updateUserAuthorization throws EntityNotFoundException when target user does not exist")
    void updateUserAuthorization_shouldThrowEntityNotFoundException_WhenTargetUserDoesNotExist() {
        // Arrange
        Long nonExistingUserId = 999L;

        when(userRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());

        Long actorId = 7L;

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserAuthorization(actorId, nonExistingUserId, UserAuthorization.ADMIN))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("Checking if updateUserAuthorization changes role and returns updated UserDTO")
    void updateUserAuthorization_shouldChangeAuthorization_WhenUserExists() {
        // Arrange
        Long actorId = 7L;
        Long userId = 1L;

        User targetUser = new User();
        targetUser.setId(userId);
        targetUser.setUserAuthorization(UserAuthorization.USER);

        UserDTO expectedDTO = new UserDTO(userId, "User", "user@test.com", UserAuthorization.ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);
        when(userMapper.toDTO(targetUser)).thenReturn(expectedDTO);

        // Act
        UserDTO result = userService.updateUserAuthorization(actorId, userId, UserAuthorization.ADMIN);

        // Assert
        assertThat(result).isEqualTo(expectedDTO);
        assertThat(targetUser.getUserAuthorization()).isEqualTo(UserAuthorization.ADMIN);
        assertThat(result.userAuthorization()).isEqualTo(UserAuthorization.ADMIN);
        verify(userRepository).save(targetUser);
        verify(userLogService).createUserLog(
                eq(actorId),
                eq(userId),
                eq(UserEventType.AUTHORIZATION_CHANGED),
                contains("USER -> ADMIN"));
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if deleteUser throws EntityNotFoundException when target user does not exist")
    void deleteUser_shouldThrowEntityNotFoundException_WhenTargetUserDoesNotExist() {
        // Arrange
        Long nonExistingUserId = 999L;

        when(userRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());

        Long actorId = 7L;

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(actorId, nonExistingUserId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("Checking if deleteUser removes the user when the user exists")
    void deleteUser_shouldDeleteUser_WhenUserExists() {
        // Arrange
        Long actorId = 7L;
        Long userId = 1L;

        User targetUser = new User();
        targetUser.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));

        // Act
        userService.deleteUser(actorId, userId);

        // Assert
        verify(userRepository, times(1)).delete(targetUser);
        verify(userLogService).createUserLog(eq(actorId), eq(userId), eq(UserEventType.DELETED), anyString());
    }

    // ── validateProfileAccess ─────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if validateProfileAccess throws UnauthorizedException when a regular user accesses another user's profile")
    void validateProfileAccess_shouldThrowUnauthorizedException_WhenUserAccessesOtherProfile() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setFullName("Regular User");
        user.setEmail("user@test.com");
        user.setUsername("user@test.com");
        user.setPassword("password");
        user.setUserAuthorization(UserAuthorization.USER);
        UserPrincipal principal = new UserPrincipal(user);

        Long otherUserId = 999L;

        // Act & Assert
        assertThatThrownBy(() -> userService.validateProfileAccess(principal, otherUserId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("permission");
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if findById returns empty Optional when user ID does not exist")
    void findById_shouldReturnEmpty_WhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThat(userService.findById(999L)).isEmpty();
    }

    // ── findByEmail ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if findByEmail returns empty Optional when email does not exist")
    void findByEmail_shouldReturnEmpty_WhenEmailDoesNotExist() {
        // Arrange
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThat(userService.findByEmail("nobody@test.com")).isEmpty();
    }
}
   