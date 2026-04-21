package org.example.visacasemanagementsystem.user.service;

import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.dto.CreateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UpdateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("UserService integration tests (H2)")
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Checking if createUser persists and returns the new user when all input is valid")
    void createUser_shouldSaveUser_WhenDataIsValid() {
        // Arrange
        CreateUserDTO dto = new CreateUserDTO(
                "New Applicant",
                "newapplicant@integration.test",
                "securePass1",
                UserAuthorization.USER
        );

        // Act
        UserDTO result = userService.createUser(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
        assertThat(result.fullName()).isEqualTo("New Applicant");
        assertThat(result.email()).isEqualTo("newapplicant@integration.test");

        var savedUsers = userRepository.findAll();
        assertThat(savedUsers).anyMatch(u -> u.getEmail().equals("newapplicant@integration.test"));
    }

    @Test
    @DisplayName("Checking if updateUser changes fullName and email in the database")
    void updateUser_shouldUpdateUserFields_WhenDataIsValid() {
        // Arrange
        User user = createAndSaveValidUser();
        UpdateUserDTO dto = new UpdateUserDTO(user.getId(), "Updated Name", "updated@integration.test");

        // Act
        UserDTO result = userService.updateUser(dto);

        // Assert
        assertThat(result.fullName()).isEqualTo("Updated Name");
        assertThat(result.email()).isEqualTo("updated@integration.test");

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getFullName()).isEqualTo("Updated Name");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@integration.test");
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("Checking if updateUserAuthorization promotes a USER to ADMIN when requested by a SYSADMIN")
    void updateUserAuthorization_shouldChangeAuthorization_WhenRequestedBySysAdmin() {
        // Arrange
        User targetUser = createAndSaveValidUser();

        // Act
        UserDTO result = userService.updateUserAuthorization(
                targetUser.getId(), UserAuthorization.ADMIN
        );

        // Assert
        assertThat(result.userAuthorization()).isEqualTo(UserAuthorization.ADMIN);

        User updated = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(updated.getUserAuthorization()).isEqualTo(UserAuthorization.ADMIN);
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("Checking if deleteUser removes the user row from the database when requested by a SYSADMIN")
    void deleteUser_shouldRemoveUser_WhenRequestedBySysAdmin() {
        // Arrange
        User targetUser = createAndSaveValidUser();
        Long targetId = targetUser.getId();

        // Act
        userService.deleteUser(targetId);

        // Assert
        assertThat(userRepository.findById(targetId)).isEmpty();
    }

    @Test
    @DisplayName("Checking if findAll returns a list that includes every seeded user")
    void findAll_shouldReturnAllUsers() {
        // Arrange
        createAndSaveValidUser();
        createAndSaveUser("Admin", UserAuthorization.ADMIN);

        // Act
        var result = userService.findAll();

        // Assert
        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Checking if findById returns the correct user when the ID exists")
    void findById_shouldReturnUser_WhenUserExists() {
        // Arrange
        User user = createAndSaveValidUser();

        // Act
        var result = userService.findById(user.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().fullName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Checking if findByEmail returns the correct user when the email exists")
    void findByEmail_shouldReturnUser_WhenEmailExists() {
        // Arrange
        User user = createAndSaveValidUser();

        // Act
        var result = userService.findByEmail(user.getEmail());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("Checking if validateProfileAccess allows a user to view their own profile")
    void validateProfileAccess_shouldNotThrow_WhenAccessingOwnProfile() {
        // Arrange
        User user = createAndSaveValidUser();
        UserPrincipal principal = new UserPrincipal(user);

        // Act & Assert — should complete without exception
        userService.validateProfileAccess(principal, user.getId());
    }

    @Test
    @DisplayName("Checking if validateProfileAccess allows a SYSADMIN to view any profile")
    void validateProfileAccess_shouldNotThrow_WhenSysAdminAccessesOtherProfile() {
        // Arrange
        User sysAdmin = createAndSaveUser("SysAdmin", UserAuthorization.SYSADMIN);
        User otherUser = createAndSaveValidUser();
        UserPrincipal principal = new UserPrincipal(sysAdmin);

        // Act & Assert — should complete without exception
        userService.validateProfileAccess(principal, otherUser.getId());
    }

    @Test
    @DisplayName("Checking if validateProfileAccess rejects a regular user viewing another user's profile")
    void validateProfileAccess_shouldThrowUnauthorizedException_WhenUserAccessesOtherProfile() {
        // Arrange
        User user = createAndSaveValidUser();
        User otherUser = createAndSaveUser("Other", UserAuthorization.ADMIN);
        UserPrincipal principal = new UserPrincipal(user);

        // Act & Assert
        Long otherUserId = otherUser.getId();
        assertThatExceptionOfType(UnauthorizedException.class)
                .isThrownBy(() -> userService.validateProfileAccess(principal, otherUserId));
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    private User createAndSaveValidUser() {
        User user = new User();
        String uniqueEmail = java.util.UUID.randomUUID() + "@test.com";
        user.setFullName("Test User");
        user.setEmail(uniqueEmail);
        user.setUsername(uniqueEmail);
        user.setPassword("password123");
        user.setUserAuthorization(UserAuthorization.USER);
        return userRepository.save(user);
    }

    private User createAndSaveUser(String name, UserAuthorization auth) {
        User user = new User();
        String uniqueEmail = java.util.UUID.randomUUID() + "@test.com";
        user.setFullName(name);
        user.setEmail(uniqueEmail);
        user.setUsername(uniqueEmail);
        user.setPassword("password123");
        user.setUserAuthorization(auth);
        return userRepository.save(user);
    }
}
