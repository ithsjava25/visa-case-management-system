package org.example.visacasemanagementsystem.comment;

import org.example.visacasemanagementsystem.comment.dto.CommentDTO;
import org.example.visacasemanagementsystem.comment.dto.CreateCommentDTO;
import org.example.visacasemanagementsystem.comment.entity.Comment;
import org.example.visacasemanagementsystem.comment.mapper.CommentMapper;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CommentMapperTest {

    private CommentMapper commentMapper;

    @BeforeEach
    void setUp() {
        commentMapper = new CommentMapper();
    }

    @Test
    void shouldMapCreateCommentDTOtoEntity() {
        // Arrange
        CreateCommentDTO dto = new CreateCommentDTO(
                100L,
                50L,
                "This is a comment text."

        );

        // Act
        Comment result = commentMapper.toEntity(dto);

        // Assert
       assertThat(result).isNotNull();
       assertThat(result.getText()).isEqualTo("This is a comment text.");
       assertThat(result.getId()).isNull();
    }

    @Test
    void shouldMapToDTOWithNullRelations() {
        // Arrange
        Comment comment= new Comment();
        comment.setId(1L);
        comment.setText("No relations");
        comment.setAuthor(null);
        comment.setVisa(null);

        // Act
        CommentDTO result = commentMapper.toDTO(comment);

        // Assert
       assertThat(result.authorName()).isEqualTo("System");
       assertThat(result.visaId()).isNull();
    }


    @Test
    void shouldMapCommentEntityToCommentDTO() {
        // Arrange
        User author = new User();
        author.setFullName("Test User");

        Visa visa = new Visa();
        visa.setId(100L);

        Comment comment = new Comment();
        comment.setId(1L);
        comment.setText("This is a comment text.");
        comment.setAuthor(author);
        comment.setVisa(visa);
        comment.setCreatedAt(LocalDateTime.now());

        // Act
        CommentDTO result = commentMapper.toDTO(comment);

        // Assert
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.text()).isEqualTo("This is a comment text.");
        assertThat(result.visaId()).isEqualTo(100L);
        assertThat(result.authorName()).isEqualTo("Test User");
        assertThat(result.createdAt()).isNotNull();

    }
}
