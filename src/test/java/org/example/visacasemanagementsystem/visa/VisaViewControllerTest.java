package org.example.visacasemanagementsystem.visa;

import org.example.visacasemanagementsystem.comment.dto.CommentDTO;
import org.example.visacasemanagementsystem.comment.service.CommentService;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.service.UserService;
import org.example.visacasemanagementsystem.visa.controller.VisaViewController;
import org.example.visacasemanagementsystem.visa.dto.CreateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.UpdateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.VisaDTO;
import org.example.visacasemanagementsystem.visa.service.VisaService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VisaViewController.class)
class VisaViewControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    private VisaService visaService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private CommentService commentService;

    @Test
    @WithMockUser
    void showDashboard_AsAdmin_ShouldReturnAllVisas() throws Exception {
        // Arrange
        Long adminId = 1L;
        UserDTO admin = createMockUser(adminId, UserAuthorization.ADMIN);

        when(userService.findById(adminId)).thenReturn(Optional.of(admin));
        when(visaService.findAll()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/visas/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("visa/dashboard"))
                .andExpect(model().attributeExists("visas"))
                .andExpect(model().attribute("currentUser", admin));
    }

    @Test
    @WithMockUser
    void showDashboard_AsUser_ShouldReturnUserVisas() throws Exception {
        // Arrange
        Long userId = 1L;
        UserDTO applicantUser = createMockUser(userId, UserAuthorization.USER);

        when(userService.findById(userId)).thenReturn(Optional.of(applicantUser));
        when(visaService.findVisasByApplicant(userId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/visas/dashboard")
                .param("currentUserId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("visa/dashboard"))
                .andExpect(model().attributeExists("visas"))
                .andExpect(model().attribute("currentUser", applicantUser));

        verify(visaService, Mockito.never()).findAll();
        verify(visaService).findVisasByApplicant(userId);

    }

    @Test
    @WithMockUser
    void showApplyForm_ShouldReturnApplyViewWithEmptyDto() throws Exception {
        // Arrange
        Long userId = 1L;
        UserDTO mockUser = createMockUser(userId, UserAuthorization.USER);

        when(userService.findById(userId)).thenReturn(Optional.of(mockUser));

        // Act & Assert
        mockMvc.perform(get("/visas/apply"))
                .andExpect(status().isOk())
                .andExpect(view().name("visa/apply-form"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attribute("currentUser", mockUser))
                .andExpect(model().attributeExists("visaTypes"))
                .andExpect(model().attributeExists("createVisaDTO"));

    }

    @Test
    @WithMockUser
    void submitApplication_WithValidData_ShouldRedirectToDashboard() throws Exception {
        // Arrange
        Long userId = 1L;
        UserDTO mockUser = createMockUser(userId, UserAuthorization.USER);

        // Act & Assert
        mockMvc.perform(post("/visas/apply")
                .param("currentUserId", userId.toString())
                .param("visaType", "TOURIST")
                .param("nationality", "Swedish")
                .param("passportNumber", "ABC12345")
                .param("travelDate", "2026-12-01")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/visas/dashboard"));

        verify(visaService).applyForVisa(any(CreateVisaDTO.class), eq(userId));
    }

    @Test
    @WithMockUser
    void submitApplication_WithInvalidData_ShouldReturnForm() throws Exception {
        // Arrange
        Long userId = 1L;
        UserDTO mockUser = createMockUser(userId, UserAuthorization.USER);

        when(userService.findById(userId)).thenReturn(Optional.of(mockUser));

        // Act
        var result = mockMvc.perform(post("/visas/apply")
                .param("currentUserId", userId.toString())
                .param("visaType", "TOURIST")
                .param("nationality", "")
                .param("passportNumber", "CDE12345")
                .param("travelDate", "2026-12-01")
                .with(csrf()));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(view().name("visa/apply-form"))
                .andExpect(model().attributeHasFieldErrors("createVisaDTO", "nationality"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("visaTypes"));

        verifyNoInteractions(visaService);

    }

    @Test
    @WithMockUser
    void showEditForm_AsCurrentUser_ShouldReturnEditView() throws Exception {
        // Arrange
        Long visaId = 100L;
        Long userId = 1L;

        when(userService.findById(userId)).thenReturn(Optional.of(createMockUser(userId, UserAuthorization.USER)));
        when(visaService.findVisaDtoById(visaId)).thenReturn(createMockVisa(visaId, userId));

        // Act
        var result = mockMvc.perform(get("/visas/{id}/edit", visaId));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(view().name("visa/edit-form"))
                .andExpect(model().attributeExists("updateVisaDto"))
                .andExpect(model().attributeExists("statusInformation"))
                .andExpect(model().attribute("isEdit", true));
    }

    @Test
    @WithMockUser
    void showEditForm_AsWrongUser_ShouldThrowUnauthorizedException() throws Exception {
        // Arrange
        Long visaId = 100L;
        Long loggedInUserId = 1L;
        Long actualUserId = 999L;

        when(userService.findById(loggedInUserId)).thenReturn(Optional.of(createMockUser(loggedInUserId, UserAuthorization.USER)));
        when(visaService.findVisaDtoById(visaId)).thenReturn(createMockVisa(visaId, actualUserId));

        // Act & Assert
        mockMvc.perform(get("/visas/{id}/edit", visaId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void processUpdate_Success_ShouldRedirectToDetails() throws Exception {
        // Arrange
        Long  visaId = 100L;
        Long userId = 1L;
        String expectedUrl = "/visas/" + visaId;

        when(userService.findById(userId)).thenReturn(Optional.of(createMockUser(userId, UserAuthorization.USER)));
        when(visaService.findVisaDtoById(visaId)).thenReturn(createMockVisa(visaId, userId));

        // Act
        var result = mockMvc.perform(post("/visas/{id}/edit", visaId)
                .param("id", visaId.toString())
                .param("currentUserId", userId.toString())
                .param("visaType", "TOURIST")
                .param("visaStatus", "INCOMPLETE")
                .param("nationality", "Sweden")
                .param("passportNumber", "ABC12345")
                .param("travelDate", "2026-12-01")
                .with(csrf()));

        // Assert
        result.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedUrl));

        verify(visaService).updateVisa(eq(visaId), any(UpdateVisaDTO.class), eq(userId));

    }

    @Test
    @WithMockUser
    void processUpdate_ValidationErrors_ShouldReturnEditForm() throws Exception {
        // Arrange
        Long visaId = 100L;
        Long userId = 1L;

        when(userService.findById(userId)).thenReturn(Optional.of(createMockUser(userId, UserAuthorization.USER)));
        when(visaService.findVisaDtoById(visaId)).thenReturn(createMockVisa(visaId, userId));
        // Act
        var result = mockMvc.perform(post("/visas/{id}/edit", visaId)
                .param("id", visaId.toString())
                .param("currentUserId", userId.toString())
                .param("visaType", "TOURIST")
                .param("visaStatus", "INCOMPLETE")
                .param("nationality", "")
                .param("passportNumber", "ABC12345")
                .param("travelDate", "2026-12-01")
                .with(csrf()));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(view().name("visa/edit-form"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attributeExists("statusInformation"));
    }

    @Test
    @WithMockUser
    void processUpdate_Unauthorized_ShouldReturnForbiddenStatus() throws Exception {
        // Arrange
        Long visaId = 100L;
        Long userId = 1L;
        UserDTO mockUser = createMockUser(userId, UserAuthorization.USER);

        doThrow(new UnauthorizedException("You are not authorized to update this application."))
                .when(visaService).updateVisa(eq(visaId), any(UpdateVisaDTO.class), eq(userId));

        // Act
        var result = mockMvc.perform(post("/visas/{id}/edit", visaId)
                .param("id", visaId.toString())
                .param("currentUserId", userId.toString())
                .param("visaType", "TOURIST")
                .param("visaStatus", "INCOMPLETE")
                .param("nationality", "Sweden")
                .param("passportNumber", "ABC12345")
                .param("travelDate", "2026-12-01")
                .with(csrf()));

        // Assert
        result.andExpect(status().isForbidden())
                .andExpect(view().name("error/error"))
                .andExpect(model().attribute("errorTitle", "⚠️Access Denied."))
                .andExpect(model().attribute("errorMessage", "You do not have permission to perform this action."));
    }

    @Test
    @WithMockUser
    void processUpdate_InvalidDate_ShouldReturnEditFormWithErrorMessage() throws Exception {
        // Arrange
        Long visaId = 100L;
        Long userId = 1L;

        when(userService.findById(userId)).thenReturn(Optional.of(createMockUser(userId, UserAuthorization.USER)));

        when(visaService.findVisaDtoById(visaId)).thenReturn(createMockVisa(visaId, userId));

        doThrow(new IllegalArgumentException("Travel date cannot be in the past."))
                .when(visaService).updateVisa(eq(visaId), any(UpdateVisaDTO.class), eq(userId));

        // Act
        var result = mockMvc.perform(post("/visas/{id}/edit", visaId)
                .param("id", visaId.toString())
                .param("currentUserId", userId.toString())
                .param("visaType", "TOURIST")
                .param("visaStatus", "INCOMPLETE")
                .param("nationality", "Sweden")
                .param("passportNumber", "ABC12345")
                .param("travelDate", "2020-01-01")
                .with(csrf()));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(view().name("visa/edit-form"))
                .andExpect(model().hasErrors());

    }

    @Test
    @WithMockUser
    void approveVisa_AsAdmin_ShouldRedirectToDetailsView() throws Exception {
        // Arrange
        Long visaId = 100L;
        Long adminId = 1L;
        UserDTO mockAdmin = createMockUser(adminId, UserAuthorization.ADMIN);

        when(visaService.approveVisa(visaId,adminId)).thenReturn(null);

        // Act
        var result = mockMvc.perform(post("/visas/{id}/approve", visaId)
                .with(csrf()));

        // Assert
        result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/visas/" + visaId));

        verify(visaService, times(1)).approveVisa(visaId,adminId);
    }

    @Test
    @WithMockUser
    void assignCaseToHandler_Success_ShouldRedirectToDetails() throws Exception {
        // Arrange
        Long  visaId = 100L;
        Long adminId = 1L;
        UserDTO mockAdmin = createMockUser(adminId, UserAuthorization.ADMIN);
        String expectedUrl = "/visas/" + visaId;

        // Act & Assert
        mockMvc.perform(post("/visas/{id}/assign",visaId)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedUrl));

        verify(visaService).assignHandler(visaId,adminId);

    }

    @Test
    @WithMockUser
    void requestMoreInformation_Success_ShouldRedirectToDetails() throws Exception {
        // Arrange
        Long visaId = 100L;
        Long adminId = 1L;
        UserDTO mockAdmin = createMockUser(adminId, UserAuthorization.ADMIN);
        String reason = "Please upload a clearer image of your passport";
        String expectedUrl = "/visas/" + visaId;

        // Act & Assert
        mockMvc.perform(post("/visas/{id}/request-info", visaId)
                        .param("reason", reason)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedUrl));

        verify(visaService).requestMoreInformation(visaId,adminId,reason);

    }

    @Test
    @WithMockUser
    void rejectVisa_AsAdmin_ShouldRedirectToDetailsView() throws Exception {
        // Arrange
        Long visaId = 100L;
        Long adminId = 1L;
        UserDTO mockAdmin = createMockUser(adminId, UserAuthorization.ADMIN);
        String reason = "Missing details about your purpose of travel";

        when(visaService.rejectVisa(visaId,adminId,reason)).thenReturn(null);

        // Act
        var result = mockMvc.perform(post("/visas/{id}/reject", visaId)
                .param("reason", reason)
                .with(csrf()));

        // Assert
        result.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/visas/" + visaId));

        verify(visaService, times(1)).rejectVisa(visaId,adminId,reason);

    }

    @Test
    @WithMockUser
    void viewDetails_AsCurrentUser_ShouldReturnDetailViewWithComments() throws Exception {
        // Arrange
        Long  visaId = 100L;
        Long userId = 1L;
        UserDTO mockUser = createMockUser(userId, UserAuthorization.USER);

        when(userService.findById(userId)).thenReturn(Optional.of(mockUser));

        VisaDTO mockVisa = createMockVisa(visaId, userId);

       when(visaService.findVisaDtoById(visaId)).thenReturn(mockVisa);

       var mockComments = List.of(new CommentDTO(1L,100L,"Admin", "Looks good!", LocalDateTime.now()));
       when(commentService.getCommentsByVisaId(visaId)).thenReturn(mockComments);

       // Act
        var result = mockMvc.perform(get("/visas/{id}", visaId));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(view().name("visa/details"))
                .andExpect(model().attribute("visa", mockVisa))
                .andExpect(model().attribute("comments", mockComments))
                .andExpect(model().attribute("currentUser", mockUser));


    }

    @Test
    @WithMockUser
    void viewDetails_AsWrongUser_ShouldThrowUnauthorizedException() throws Exception {
        // Arrange
        Long visaId = 100L;
        Long hackerId = 1L;
        Long userId = 99L;

        UserDTO hacker = createMockUser(hackerId, UserAuthorization.USER);

        VisaDTO othersVisa = createMockVisa(visaId, userId);

        when(userService.findById(hackerId)).thenReturn(Optional.of(hacker));
        when(visaService.findVisaDtoById(visaId)).thenReturn(othersVisa);

        // Act & Assert
        mockMvc.perform(get("/visas/{id}", visaId))
                .andExpect(status().isForbidden());

    }

    @Test
    @WithMockUser
    void viewDetails_AsAdmin_ShouldAllowViewingOtherVisas() throws Exception {
        // Arrange
        Long visaId = 100L;
        Long adminId = 50L;
        Long applicantId = 1L;

        UserDTO mockAdmin = createMockUser(adminId, UserAuthorization.ADMIN);
        VisaDTO applicantVisa = createMockVisa(visaId, applicantId);

        when(userService.findById(adminId)).thenReturn(Optional.of(mockAdmin));
        when(visaService.findVisaDtoById(visaId)).thenReturn(applicantVisa);
        when(commentService.getCommentsByVisaId(visaId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/visas/{id}", visaId))
                .andExpect(status().isOk())
                .andExpect(view().name("visa/details"));

    }

    // Helper methods
    private UserDTO createMockUser(Long id, UserAuthorization role) {
        User testUser = new User();
        testUser.setId(id);
        testUser.setUsername("test@test.com");
        testUser.setEmail("test@test.com");
        testUser.setPassword("password");
        testUser.setUserAuthorization(role);
        UserPrincipal principal = new UserPrincipal(testUser);
        Authentication authentication = new TestingAuthenticationToken(principal, "password", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return  new UserDTO(id,"Test User", "test@test.com", role);
    }

    private VisaDTO createMockVisa(Long visaId, Long applicantId) {
        return new VisaDTO(
                visaId,
                VisaType.TOURIST,
                VisaStatus.INCOMPLETE,
                "Sweden",
                "ABC12345",
                LocalDate.now().plusMonths(1),
                applicantId,
                "Test User",
                null, null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "Some status info"
        );
    }



}
