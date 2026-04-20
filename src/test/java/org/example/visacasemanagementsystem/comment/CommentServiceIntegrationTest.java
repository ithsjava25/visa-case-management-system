package org.example.visacasemanagementsystem.comment;

import org.example.visacasemanagementsystem.comment.dto.CommentDTO;
import org.example.visacasemanagementsystem.comment.dto.CreateCommentDTO;
import org.example.visacasemanagementsystem.comment.service.CommentService;
import org.example.visacasemanagementsystem.exception.ResourceNotFoundException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.visa.VisaStatus;
import org.example.visacasemanagementsystem.visa.VisaType;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.example.visacasemanagementsystem.visa.repository.VisaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CommentServiceIntegrationTest {

    @Autowired
    private CommentService commentService;
    @Autowired
    private VisaRepository visaRepository;
    @Autowired
    private UserRepository userRepository;

    private User testUser;
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

    @Test
    void createComment_shouldSaveAndRetrieveComment() {
        // Arrange
        CreateCommentDTO dto = new CreateCommentDTO(
                testVisa.getId(),
                testUser.getId(),
                "This is a test comment"
        );

        // Act
        CommentDTO savedComment = commentService.createComment(dto);

        // Assert
         assertThat(savedComment).isNotNull();
         assertThat(savedComment.id()).isNotNull();
         assertThat(savedComment.text()).isEqualTo("This is a test comment");

        List<CommentDTO> comments = commentService.getCommentsByVisaId(testVisa.getId());

        assertThat(comments).isNotEmpty();
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).text()).isEqualTo("This is a test comment");

    }

    @Test
    void createComment_shouldThrowException_WhenUserDoesNotExist() {
        // Arrange
        Long nonExistentUserId = 999L;
        CreateCommentDTO dto = new CreateCommentDTO(testVisa.getId(), nonExistentUserId, "Valid comment text");

        // Act & Assert
        assertThatThrownBy(() -> commentService.createComment(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: " + nonExistentUserId);
    }

    @Test
    void createComment_shouldThrowException_WhenVisaDoesNotExist() {
        // Arrange
        Long nonExistentVisaId = 999L;
        CreateCommentDTO dto = new CreateCommentDTO(nonExistentVisaId, testUser.getId(), "Some text");

        // Act & Assert
        assertThatThrownBy(() -> commentService.createComment(dto))
        .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Visa case not found with id: " + nonExistentVisaId);
    }

    @Test
    void createComment_shouldThrowException_WhenTextIsBlank() {
        // Arrange
        CreateCommentDTO dto = new CreateCommentDTO(testVisa.getId(), testUser.getId(), " ");

        // Act & Assert
        assertThatThrownBy(() -> commentService.createComment(dto))
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
}
