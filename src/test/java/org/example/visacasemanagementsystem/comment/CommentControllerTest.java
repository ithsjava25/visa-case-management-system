package org.example.visacasemanagementsystem.comment;

import org.example.visacasemanagementsystem.comment.controller.CommentController;
import org.example.visacasemanagementsystem.comment.dto.CommentDTO;
import org.example.visacasemanagementsystem.comment.dto.CreateCommentDTO;
import org.example.visacasemanagementsystem.comment.service.CommentService;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;



@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CommentService commentService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void createComment_shouldReturnCreated() throws Exception {
        // Arrange
        CreateCommentDTO createDto= new CreateCommentDTO(1L, "Test message");
        CommentDTO responseDto = new CommentDTO(1L,"Test User", "Test message", LocalDateTime.now());

        when(commentService.createComment(any(CreateCommentDTO.class))).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/comments")
                        .with(csrf())
                .contentType((MediaType.APPLICATION_JSON))
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visaId").value(1L))
                .andExpect(jsonPath("$.text").value("Test message"))
                .andExpect(jsonPath("$.authorName").value("Test User"));
    }

    @Test
    @WithMockUser
    void getCommentsByVisa_ShouldReturnList() throws Exception {
        // Arrange
        String expectedText = "Hello World";
        CommentDTO comment = new CommentDTO(1L, "Admin", expectedText, LocalDateTime.now());

        when(commentService.getCommentsByVisaId(1L)).thenReturn(List.of(comment));

        // Act & Assert
        mockMvc.perform(get("/api/comments/visa/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].text").value(expectedText));
    }
}
