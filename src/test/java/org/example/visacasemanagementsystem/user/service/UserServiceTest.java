package org.example.visacasemanagementsystem.user.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.visacasemanagementsystem.TestcontainersConfiguration;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.dto.CreateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UpdateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.user.security.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;
    private User savedAdmin;
    private User savedSysAdmin;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setFullName("Test User");
        user.setEmail("user@service.test");
        user.setPassword("password123");
        user.setUserAuthorization(UserAuthorization.USER);
        savedUser = userRepository.save(user);

        User admin = new User();
        admin.setFullName("Test Admin");
        admin.setEmail("admin@service.test");
        admin.setPassword("password123");
        admin.setUserAuthorization(UserAuthorization.ADMIN);
        savedAdmin = userRepository.save(admin);

        User sysadmin = new User();
        sysadmin.setFullName("Test SysAdmin");
        sysadmin.setEmail("sysadmin@service.test");
        sysadmin.setPassword("password123");
        sysadmin.setUserAuthorization(UserAuthorization.SYSADMIN);
        savedSysAdmin = userRepository.save(sysadmin);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_ReturnsListContainingAllSeededUsers() {
        List<UserDTO> result = userService.findAll();

        assertNotNull(result);
        assertTrue(result.size() >= 3);
        assertTrue(result.stream().anyMatch(u -> u.email().equals("user@service.test")));
        assertTrue(result.stream().anyMatch(u -> u.email().equals("admin@service.test")));
        assertTrue(result.stream().anyMatch(u -> u.email().equals("sysadmin@service.test")));
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_WithExistingId_ReturnsMatchingUser() {
        Optional<UserDTO> result = userService.findById(savedUser.getId());

        assertTrue(result.isPresent());
        assertEquals("Test User", result.get().fullName());
        assertEquals("user@service.test", result.get().email());
        assertEquals(UserAuthorization.USER, result.get().userAuthorization());
    }

    @Test
    void findById_WithNonExistingId_ReturnsEmpty() {
        Optional<UserDTO> result = userService.findById(999999L);
        assertFalse(result.isPresent());
    }

    // ── findByEmail ───────────────────────────────────────────────────────────

    @Test
    void findByEmail_WithExistingEmail_ReturnsMatchingUser() {
        Optional<UserDTO> result = userService.findByEmail("admin@service.test");

        assertTrue(result.isPresent());
        assertEquals("Test Admin", result.get().fullName());
        assertEquals(UserAuthorization.ADMIN, result.get().userAuthorization());
    }

    @Test
    void findByEmail_WithNonExistingEmail_ReturnsEmpty() {
        Optional<UserDTO> result = userService.findByEmail("nobody@nowhere.test");
        assertFalse(result.isPresent());
    }

    // ── createUser ────────────────────────────────────────────────────────────

    @Test
    void createUser_WithValidData_PersistsAndReturnsNewUser() {
        CreateUserDTO dto = new CreateUserDTO(
                "New Applicant",
                "newapplicant@service.test",
                "securePass1",
                UserAuthorization.USER
        );

        UserDTO result = userService.createUser(dto);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("New Applicant", result.fullName());
        assertEquals("newapplicant@service.test", result.email());
    }

    @Test
    void createUser_WithDuplicateEmail_ThrowsIllegalArgumentException() {
        CreateUserDTO dto = new CreateUserDTO(
                "Duplicate",
                "user@service.test",
                "password123",
                UserAuthorization.USER
        );

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(dto));
    }

    @Test
    void createUser_WithShortPassword_ThrowsIllegalArgumentException() {
        CreateUserDTO dto = new CreateUserDTO(
                "Short Pass",
                "shortpass@service.test",
                "abc",
                UserAuthorization.USER
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(dto)
        );
        assertTrue(ex.getMessage().contains("8 characters"));
    }

    // ── updateUser ────────────────────────────────────────────────────────────

    @Test
    void updateUser_WithValidData_UpdatesAndReturnsUser() {
        UpdateUserDTO dto = new UpdateUserDTO(
                savedUser.getId(),
                "Updated Name",
                "updated@service.test"
        );

        UserDTO result = userService.updateUser(dto);

        assertNotNull(result);
        assertEquals("Updated Name", result.fullName());
        assertEquals("updated@service.test", result.email());
    }

    @Test
    void updateUser_WithNonExistingId_ThrowsEntityNotFoundException() {
        UpdateUserDTO dto = new UpdateUserDTO(999999L, "Name", "missing@service.test");

        assertThrows(EntityNotFoundException.class, () -> userService.updateUser(dto));
    }

    @Test
    void updateUser_WithEmailAlreadyOwnedByAnotherUser_ThrowsIllegalArgumentException() {
        // Try to update savedUser's email to admin's existing email
        UpdateUserDTO dto = new UpdateUserDTO(
                savedUser.getId(),
                "Test User",
                "admin@service.test"
        );

        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(dto));
    }

    // ── updateUserAuthorization ───────────────────────────────────────────────

    @Test
    void updateUserAuthorization_BySysAdmin_ChangesAuthorizationLevel() {
        UserDTO result = userService.updateUserAuthorization(
                savedUser.getId(),
                UserAuthorization.ADMIN,
                savedSysAdmin.getId()
        );

        assertEquals(UserAuthorization.ADMIN, result.userAuthorization());
    }

    @Test
    void updateUserAuthorization_ByNonSysAdmin_ThrowsUnauthorizedException() {
        assertThrows(UnauthorizedException.class, () ->
                userService.updateUserAuthorization(
                        savedAdmin.getId(),
                        UserAuthorization.SYSADMIN,
                        savedUser.getId()
                )
        );
    }

    @Test
    void updateUserAuthorization_WithNonExistingTargetUser_ThrowsEntityNotFoundException() {
        assertThrows(EntityNotFoundException.class, () ->
                userService.updateUserAuthorization(
                        999999L,
                        UserAuthorization.ADMIN,
                        savedSysAdmin.getId()
                )
        );
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    void deleteUser_BySysAdmin_RemovesUserFromDatabase() {
        Long userId = savedUser.getId();

        userService.deleteUser(userId, savedSysAdmin.getId());

        assertFalse(userRepository.findById(userId).isPresent());
    }

    @Test
    void deleteUser_ByNonSysAdmin_ThrowsUnauthorizedException() {
        assertThrows(UnauthorizedException.class, () ->
                userService.deleteUser(savedAdmin.getId(), savedUser.getId())
        );
    }

    @Test
    void deleteUser_WithNonExistingUser_ThrowsEntityNotFoundException() {
        assertThrows(EntityNotFoundException.class, () ->
                userService.deleteUser(999999L, savedSysAdmin.getId())
        );
    }

    // ── validateProfileAccess ─────────────────────────────────────────────────

    @Test
    void validateProfileAccess_OwnProfile_DoesNotThrow() {
        SecurityUser principal = new SecurityUser(savedUser);

        assertDoesNotThrow(() -> userService.validateProfileAccess(principal, savedUser.getId()));
    }

    @Test
    void validateProfileAccess_SysAdminViewingOtherProfile_DoesNotThrow() {
        SecurityUser principal = new SecurityUser(savedSysAdmin);

        assertDoesNotThrow(() -> userService.validateProfileAccess(principal, savedUser.getId()));
    }

    @Test
    void validateProfileAccess_UserViewingOtherProfile_ThrowsUnauthorizedException() {
        SecurityUser principal = new SecurityUser(savedUser);

        assertThrows(UnauthorizedException.class, () ->
                userService.validateProfileAccess(principal, savedAdmin.getId())
        );
    }

    // ── validateSysAdmin (SecurityUser overload) ──────────────────────────────

    @Test
    void validateSysAdmin_WithSysAdminPrincipal_DoesNotThrow() {
        SecurityUser principal = new SecurityUser(savedSysAdmin);

        assertDoesNotThrow(() -> userService.validateSysAdmin(principal));
    }

    @Test
    void validateSysAdmin_WithAdminPrincipal_ThrowsUnauthorizedException() {
        SecurityUser principal = new SecurityUser(savedAdmin);

        assertThrows(UnauthorizedException.class, () -> userService.validateSysAdmin(principal));
    }

    @Test
    void validateSysAdmin_WithUserPrincipal_ThrowsUnauthorizedException() {
        SecurityUser principal = new SecurityUser(savedUser);

        assertThrows(UnauthorizedException.class, () -> userService.validateSysAdmin(principal));
    }

    // ── validateAdmin ─────────────────────────────────────────────────────────

    @Test
    void validateAdmin_WithAdminPrincipal_DoesNotThrow() {
        SecurityUser principal = new SecurityUser(savedAdmin);

        assertDoesNotThrow(() -> userService.validateAdmin(principal));
    }

    @Test
    void validateAdmin_WithSysAdminPrincipal_DoesNotThrow() {
        SecurityUser principal = new SecurityUser(savedSysAdmin);

        assertDoesNotThrow(() -> userService.validateAdmin(principal));
    }

    @Test
    void validateAdmin_WithUserPrincipal_ThrowsUnauthorizedException() {
        SecurityUser principal = new SecurityUser(savedUser);

        assertThrows(UnauthorizedException.class, () -> userService.validateAdmin(principal));
    }
}
