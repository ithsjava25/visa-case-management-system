package org.example.visacasemanagementsystem.user.mapper;

import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.dto.CreateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UpdateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("UserMapper unit tests")
class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
    }

    @Test
    @DisplayName("Checking if toDTO maps a User entity to a UserDTO with all fields preserved")
    void shouldMapUserEntityToUserDTO() {
        // Arrange
        User user = new User();
        user.setFullName("Alice Wonderland");
        user.setEmail("alice@example.com");
        user.setUsername("alice@example.com");
        user.setPassword("password123");
        user.setUserAuthorization(UserAuthorization.USER);

        // Act
        UserDTO dto = userMapper.toDTO(user);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.fullName()).isEqualTo("Alice Wonderland");
        assertThat(dto.email()).isEqualTo("alice@example.com");
        assertThat(dto.userAuthorization()).isEqualTo(UserAuthorization.USER);
    }

    @Test
    @DisplayName("Checking if toDTO returns null when the input User is null")
    void shouldReturnNullDTO_WhenUserIsNull() {
        // Act & Assert
        assertThat(userMapper.toDTO(null)).isNull();
    }

    @Test
    @DisplayName("Checking if toEntity maps a CreateUserDTO to a User entity with correct name, email, username, and authorization")
    void shouldMapCreateUserDTOToUserEntity() {
        // Arrange
        CreateUserDTO dto = new CreateUserDTO(
                "Bob Builder",
                "bob@example.com",
                "password123",
                UserAuthorization.ADMIN
        );

        // Act
        User user = userMapper.toEntity(dto);

        // Assert
        assertThat(user).isNotNull();
        assertThat(user.getFullName()).isEqualTo("Bob Builder");
        assertThat(user.getEmail()).isEqualTo("bob@example.com");
        assertThat(user.getUsername()).isEqualTo("bob@example.com");
        assertThat(user.getUserAuthorization()).isEqualTo(UserAuthorization.ADMIN);
    }

    @Test
    @DisplayName("Checking if toEntity does not set the password field (UserService handles passwords separately)")
    void shouldNotSetPassword_WhenMappingFromCreateUserDTO() {
        // Arrange
        CreateUserDTO dto = new CreateUserDTO(
                "No Pass",
                "nopass@example.com",
                "supersecret",
                UserAuthorization.USER
        );

        // Act
        User user = userMapper.toEntity(dto);

        // Assert
        assertThat(user).isNotNull();
        assertThat(user.getPassword()).isNull();
    }

    @Test
    @DisplayName("Checking if toEntity returns null when the input CreateUserDTO is null")
    void shouldReturnNullEntity_WhenCreateUserDTOIsNull() {
        // Act & Assert
        assertThat(userMapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("Checking if updateEntityFromDTO overwrites fullName on an existing User entity")
    void shouldUpdateExistingUserEntityFromUpdateUserDTO() {
        // Arrange
        User user = new User();
        user.setFullName("Old Name");
        user.setEmail("old@example.com");
        user.setUsername("old@example.com");
        user.setPassword("password123");
        user.setUserAuthorization(UserAuthorization.USER);

        UpdateUserDTO dto = new UpdateUserDTO(1L, "New Name", "");

        // Act
        userMapper.updateEntityFromDTO(dto, user);

        // Assert
        assertThat(user.getFullName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("Checking if updateEntityFromDTO does not modify authorization or password")
    void shouldNotModifyAuthorizationOrPassword_WhenUpdatingFromDTO() {
        // Arrange
        User user = new User();
        user.setFullName("Original");
        user.setEmail("original@example.com");
        user.setUsername("original@example.com");
        user.setPassword("secret123");
        user.setUserAuthorization(UserAuthorization.SYSADMIN);

        UpdateUserDTO dto = new UpdateUserDTO(1L, "Changed Name", "newPassword123");

        // Act
        userMapper.updateEntityFromDTO(dto, user);

        // Assert
        assertThat(user.getUserAuthorization()).isEqualTo(UserAuthorization.SYSADMIN);
        assertThat(user.getPassword()).isEqualTo("secret123");
    }

    @Test
    @DisplayName("Checking if updateEntityFromDTO leaves the entity unchanged when the DTO is null")
    void shouldNotUpdateEntity_WhenUpdateDTOIsNull() {
        // Arrange
        User user = new User();
        user.setFullName("Original Name");
        user.setEmail("original@example.com");
        user.setUsername("original@example.com");

        // Act
        userMapper.updateEntityFromDTO(null, user);

        // Assert
        assertThat(user.getFullName()).isEqualTo("Original Name");
        assertThat(user.getEmail()).isEqualTo("original@example.com");
        assertThat(user.getUsername()).isEqualTo("original@example.com");
    }

    @Test
    @DisplayName("Checking if updateEntityFromDTO does not throw when the User entity is null")
    void shouldNotThrow_WhenUserIsNullInUpdate() {
        // Arrange
        UpdateUserDTO dto = new UpdateUserDTO(1L, "Some Name", "somePassword1");

        // Act & Assert
        assertThatCode(() -> userMapper.updateEntityFromDTO(dto, null))
                .doesNotThrowAnyException();
    }
}
