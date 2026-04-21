package org.example.visacasemanagementsystem.comment;

import org.example.visacasemanagementsystem.comment.dto.CommentDTO;
import org.example.visacasemanagementsystem.comment.dto.CreateCommentDTO;
import org.example.visacasemanagementsystem.comment.service.CommentService;
import org.example.visacasemanagementsystem.exception.ResourceNotFoundException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.example.visacasemanagementsystem.visa.VisaStatus;
import org.example.visacasemanagementsystem.visa.VisaType;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.example.visacasemanagementsystem.visa.repository.VisaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CommentServiceIntegrationTest{

    @Autowired
    private CommentService commentService;
    @Autowired
    private VisaRepository visaRepository;
    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User nonExistentUser;
    private Visa testVisa;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser  = new User();
        testUser.setFullName("TestUser");
        testUser.setEmail("test@example.com");
        testUser.setUsername("test@example.com");
        testUser.setPassword("password123");
        testUser.setUserAuthorization(UserAuthorization.USER);
        testUser  = userRepository.save(testUser);

        // Create nonexistent user
        nonExistentUser  = new User();
        nonExistentUser.setId(Long.MAX_VALUE);
        nonExistentUser.setFullName("NonUser");
        nonExistentUser.setEmail("non@example.com");
        nonExistentUser.setUsername("non@example.com");
        nonExistentUser.setPassword("password123");
        nonExistentUser.setUserAuthorization(UserAuthorization.USER);

        // Create test visa
        testVisa = new Visa();
        testVisa.setApplicant(testUser);
        testVisa.setPassportNumber("AB123456");
        testVisa.setNationality("Swedish");
        testVisa.setVisaType(VisaType.TOURIST);
        testVisa.setVisaStatus(VisaStatus.SUBMITTED);
        testVisa.setTravelDate(LocalDate.now().plusMonths(1));
        testVisa = visaRepository.save(testVisa);

    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createComment_shouldSaveAndRetrieveComment() {
        // Arrange
        CreateCommentDTO dto = new CreateCommentDTO(
                testVisa.getId(),
                "This is a test comment"
        );

        // Act
        authenticateTestUser();
        CommentDTO savedComment = commentService.createComment(dto, testUser.getId());

        // Assert
         assertThat(savedComment).isNotNull();
         assertThat(savedComment.text()).isEqualTo("This is a test comment");

        List<CommentDTO> comments = commentService.getCommentsByVisaId(testVisa.getId());

        assertThat(comments).isNotEmpty();
        assertThat(comments).hasSize(1);
        assertThat(comments.getFirst().text()).isEqualTo("This is a test comment");

    }

    @Test
    void createComment_shouldThrowException_WhenUserDoesNotExist() {
        // Arrange
        CreateCommentDTO dto = new CreateCommentDTO(testVisa.getId(), "Valid comment text");

        // Act & Assert
        authenticateNonExistentUser();
        assertThatThrownBy(() -> commentService.createComment(dto, nonExistentUser.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: " + nonExistentUser.getId());
    }

    @Test
    void createComment_shouldThrowException_WhenVisaDoesNotExist() {
        // Arrange
        long nonExistentVisaId = 999L;
        CreateCommentDTO dto = new CreateCommentDTO(nonExistentVisaId, "Some text");

        // Act & Assert
        authenticateTestUser();
        assertThatThrownBy(() -> commentService.createComment(dto, testUser.getId()))
        .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Visa case not found with id: " + nonExistentVisaId);
    }

    @Test
    void createComment_shouldThrowException_WhenTextIsBlank() {
        // Arrange
        CreateCommentDTO dto = new CreateCommentDTO(testVisa.getId(), " ");

        // Act & Assert
        assertThatThrownBy(() -> commentService.createComment(dto, testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Comment text cannot be empty");
    }

    @Test
    void getCommentsByVisaId_ShouldThrowException_WhenIdIsInvalid() {
        // Act & Assert
        assertThatThrownBy(() -> commentService.getCommentsByVisaId(-1L))
        .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Visa ID must be a positive number");
    }

    @Test
    void getCommentsByVisaID_ShouldThrowException_WhenVisaDoesNotExistAndNoComments() {
        // Act & Assert
        assertThatThrownBy(() -> commentService.getCommentsByVisaId(999L))
        .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Visa case not found with id: 999");
    }

    @Test
    void getCommentsByVisaID_ShouldReturnEmptyList_WhenVisaExistsButHasNoComments() {
        // Act
        List<CommentDTO> comments = commentService.getCommentsByVisaId(testVisa.getId());

        // Assert
        assertThat(comments).isEmpty();

    }

    // Helper method
    private void authenticateTestUser() {
        UserPrincipal principal = new UserPrincipal(testUser);
        Authentication authentication = new TestingAuthenticationToken(principal, "password123", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void authenticateNonExistentUser() {
        UserPrincipal principal = new UserPrincipal(nonExistentUser);
        Authentication authentication = new TestingAuthenticationToken(principal, "password123", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
