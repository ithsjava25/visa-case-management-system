package org.example.visacasemanagementsystem.user.mapper;

import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.dto.CreateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UpdateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
    }

    // ── toDTO ─────────────────────────────────────────────────────────────────

    @Test
    void toDTO_WithValidUser_ReturnsMappedUserDTO() {
        User user = new User();
        user.setFullName("Alice Wonderland");
        user.setEmail("alice@example.com");
        user.setPassword("password123");
        user.setUserAuthorization(UserAuthorization.USER);

        UserDTO dto = userMapper.toDTO(user);

        assertNotNull(dto);
        assertEquals("Alice Wonderland", dto.fullName());
        assertEquals("alice@example.com", dto.email());
        assertEquals(UserAuthorization.USER, dto.userAuthorization());
    }

    @Test
    void toDTO_WithAllAuthorizationLevels_MapsCorrectly() {
        for (UserAuthorization auth : UserAuthorization.values()) {
            User user = new User();
            user.setFullName("Test User");
            user.setEmail("test@example.com");
            user.setPassword("password123");
            user.setUserAuthorization(auth);

            UserDTO dto = userMapper.toDTO(user);

            assertNotNull(dto);
            assertEquals(auth, dto.userAuthorization());
        }
    }

    @Test
    void toDTO_WithNullUser_ReturnsNull() {
        assertNull(userMapper.toDTO(null));
    }

    // ── toEntity ──────────────────────────────────────────────────────────────

    @Test
    void toEntity_WithValidCreateUserDTO_ReturnsMappedUser() {
        CreateUserDTO dto = new CreateUserDTO(
                "Bob Builder",
                "bob@example.com",
                "password123",
                UserAuthorization.ADMIN
        );

        User user = userMapper.toEntity(dto);

        assertNotNull(user);
        assertEquals("Bob Builder", user.getFullName());
        assertEquals("bob@example.com", user.getEmail());
        assertEquals(UserAuthorization.ADMIN, user.getUserAuthorization());
    }

    @Test
    void toEntity_DoesNotSetPassword() {
        // The mapper intentionally leaves password unset; UserService sets it separately
        CreateUserDTO dto = new CreateUserDTO(
                "No Pass",
                "nopass@example.com",
                "supersecret",
                UserAuthorization.USER
        );

        User user = userMapper.toEntity(dto);

        assertNotNull(user);
        assertNull(user.getPassword());
    }

    @Test
    void toEntity_WithNullCreateUserDTO_ReturnsNull() {
        assertNull(userMapper.toEntity(null));
    }

    // ── updateEntityFromDTO ───────────────────────────────────────────────────

    @Test
    void updateEntityFromDTO_WithValidInputs_UpdatesFullNameAndEmail() {
        User user = new User();
        user.setFullName("Old Name");
        user.setEmail("old@example.com");
        user.setPassword("password123");
        user.setUserAuthorization(UserAuthorization.USER);

        UpdateUserDTO dto = new UpdateUserDTO(1L, "New Name", "new@example.com");
        userMapper.updateEntityFromDTO(dto, user);

        assertEquals("New Name", user.getFullName());
        assertEquals("new@example.com", user.getEmail());
    }

    @Test
    void updateEntityFromDTO_DoesNotModifyAuthorizationOrPassword() {
        User user = new User();
        user.setFullName("Original");
        user.setEmail("original@example.com");
        user.setPassword("secret123");
        user.setUserAuthorization(UserAuthorization.SYSADMIN);

        UpdateUserDTO dto = new UpdateUserDTO(1L, "Changed Name", "changed@example.com");
        userMapper.updateEntityFromDTO(dto, user);

        // Authorization and password must remain untouched
        assertEquals(UserAuthorization.SYSADMIN, user.getUserAuthorization());
        assertEquals("secret123", user.getPassword());
    }

    @Test
    void updateEntityFromDTO_WithNullDTO_DoesNotUpdateEntity() {
        User user = new User();
        user.setFullName("Original Name");
        user.setEmail("original@example.com");

        userMapper.updateEntityFromDTO(null, user);

        assertEquals("Original Name", user.getFullName());
        assertEquals("original@example.com", user.getEmail());
    }

    @Test
    void updateEntityFromDTO_WithNullUser_DoesNotThrow() {
        UpdateUserDTO dto = new UpdateUserDTO(1L, "Some Name", "some@example.com");
        assertDoesNotThrow(() -> userMapper.updateEntityFromDTO(dto, null));
    }

    @Test
    void updateEntityFromDTO_WithBothNull_DoesNotThrow() {
        assertDoesNotThrow(() -> userMapper.updateEntityFromDTO(null, null));
    }
}
