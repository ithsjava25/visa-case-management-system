package org.example.visacasemanagementsystem.user.controller;

import org.example.visacasemanagementsystem.audit.service.AuditService;
import org.example.visacasemanagementsystem.config.SecurityConfig;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.example.visacasemanagementsystem.user.service.UserService;
import org.example.visacasemanagementsystem.visa.service.VisaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserViewController.class)
@Import(SecurityConfig.class)
@DisplayName("UserViewController web-layer tests")
class UserViewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private VisaService visaService;
    @MockitoBean
    private AuditService auditService;

    // ── GET /user/signup ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if GET /user/signup returns the signup view")
    void userSignupForm_ShouldReturnSignupView() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/user/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/signup"));
    }

    // ── POST /user/signup ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if POST /user/signup with valid data redirects to the login page")
    void createUser_WithValidData_ShouldRedirectToLogin() throws Exception {
        // Arrange
        when(userService.createUser(any())).thenReturn(
                new UserDTO(1L, "New Applicant", "new@test.com", UserAuthorization.USER)
        );

        // Act & Assert
        mockMvc.perform(post("/user/signup")
                        .param("fullName", "New Applicant")
                        .param("email", "new@test.com")
                        .param("password", "securePass1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/login"));

        verify(userService).createUser(any());
    }

    @Test
    @DisplayName("Checking if POST /user/signup with a duplicate email re-renders the signup form with an error")
    void createUser_WithDuplicateEmail_ShouldReturnSignupViewWithError() throws Exception {
        // Arrange
        when(userService.createUser(any()))
                .thenThrow(new IllegalArgumentException("A user with this email already exists"));

        // Act & Assert
        mockMvc.perform(post("/user/signup")
                        .param("fullName", "Duplicate User")
                        .param("email", "existing@test.com")
                        .param("password", "password123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("user/signup"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("Checking if POST /user/signup with a short password re-renders the signup form with an error")
    void createUser_WithShortPassword_ShouldReturnSignupViewWithError() throws Exception {
        // Arrange
        when(userService.createUser(any()))
                .thenThrow(new IllegalArgumentException("Password must be at least 8 characters"));

        // Act & Assert
        mockMvc.perform(post("/user/signup")
                        .param("fullName", "Short Pass")
                        .param("email", "short@test.com")
                        .param("password", "abc")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("user/signup"))
                .andExpect(model().attributeExists("error"));
    }

    // ── GET /user/login ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if GET /user/login returns the login view")
    void userLoginForm_ShouldReturnLoginView() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/user/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/login"));
    }

    // ── GET /profile/view/{userId} ────────────────────────────────────────────

    @Test
    @DisplayName("Checking if GET /profile/view/{id} viewed by the profile owner shows 'canEdit = true'")
    void viewProfile_AsOwnUser_ShouldReturnProfileViewWithCanEditTrue() throws Exception {
        // Arrange
        Long userId = 1L;
        UserDTO userDTO = new UserDTO(userId, "Test User", "user@test.com", UserAuthorization.USER);
        when(userService.findById(userId)).thenReturn(Optional.of(userDTO));

        // Act & Assert
        mockMvc.perform(get("/profile/view/" + userId)
                        .with(authentication(authFor(userId, "Test User", "user@test.com", UserAuthorization.USER))))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/view"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("canEdit", true));
    }

    @Test
    @DisplayName("Checking if GET /profile/view/{id} viewed by a SYSADMIN shows 'canEdit = true'")
    void viewProfile_AsSysAdmin_ShouldReturnProfileViewWithCanEditTrue() throws Exception {
        // Arrange
        Long targetUserId = 1L;
        Long sysAdminId = 99L;
        UserDTO userDTO = new UserDTO(targetUserId, "Test User", "user@test.com", UserAuthorization.USER);
        when(userService.findById(targetUserId)).thenReturn(Optional.of(userDTO));

        // Act & Assert
        mockMvc.perform(get("/profile/view/" + targetUserId)
                        .with(authentication(authFor(sysAdminId, "SysAdmin", "sysadmin@test.com", UserAuthorization.SYSADMIN))))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/view"))
                .andExpect(model().attribute("canEdit", true));
    }

    @Test
    @DisplayName("Checking if GET /profile/view/{id} returns 403 when a regular user tries to view another user's profile")
    void viewProfile_AsUnauthorizedUser_ShouldReturnForbidden() throws Exception {
        // Arrange
        Long targetUserId = 999L;

        doThrow(new UnauthorizedException("You do not have permission to edit this profile."))
                .when(userService).validateProfileAccess(any(UserPrincipal.class), eq(targetUserId));

        UserDTO userDTO = new UserDTO(targetUserId, "Other User", "other@test.com", UserAuthorization.USER);
        when(userService.findById(targetUserId)).thenReturn(Optional.of(userDTO));

        // Act & Assert
        mockMvc.perform(get("/profile/view/" + targetUserId)
                        .with(authentication(authFor(1L, "Test User", "user@test.com", UserAuthorization.USER))))
                .andExpect(status().isForbidden());
    }

    // ── GET /profile/edit/{userId} ────────────────────────────────────────────

    @Test
    @DisplayName("Checking if GET /profile/edit/{id} returns the edit form when accessed by the profile owner")
    void showProfileEditForm_AsOwnUser_ShouldReturnEditView() throws Exception {
        // Arrange
        Long userId = 1L;
        UserDTO userDTO = new UserDTO(userId, "Test User", "user@test.com", UserAuthorization.USER);
        when(userService.findById(userId)).thenReturn(Optional.of(userDTO));

        // Act & Assert
        mockMvc.perform(get("/profile/edit/" + userId)
                        .with(authentication(authFor(userId, "Test User", "user@test.com", UserAuthorization.USER))))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @DisplayName("Checking if GET /profile/edit/{id} returns 403 when accessed by an unauthorized user")
    void showProfileEditForm_AsUnauthorizedUser_ShouldReturnForbidden() throws Exception {
        // Arrange
        Long targetUserId = 999L;

        doThrow(new UnauthorizedException("You do not have permission to edit this profile."))
                .when(userService).validateProfileAccess(any(UserPrincipal.class), eq(targetUserId));

        // Act & Assert
        mockMvc.perform(get("/profile/edit/" + targetUserId)
                        .with(authentication(authFor(1L, "Test User", "user@test.com", UserAuthorization.USER))))
                .andExpect(status().isForbidden());
    }

    // ── POST /profile/edit/{userId} ───────────────────────────────────────────

    @Test
    @DisplayName("Checking if POST /profile/edit/{id} with valid data redirects to the profile view")
    void updateProfile_WithValidData_ShouldRedirectToProfileView() throws Exception {
        // Arrange
        Long userId = 1L;
        when(userService.updateUser(any())).thenReturn(
                new UserDTO(userId, "Updated Name", "updated@test.com", UserAuthorization.USER)
        );

        // Act & Assert
        mockMvc.perform(post("/profile/edit/" + userId)
                        .param("fullName", "Updated Name")
                        .param("email", "updated@test.com")
                        .with(authentication(authFor(userId, "Test User", "user@test.com", UserAuthorization.USER)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/view/" + userId));

        verify(userService).updateUser(any());
    }

    @Test
    @DisplayName("Checking if POST /profile/edit/{id} with a duplicate email re-renders the edit form with an error")
    void updateProfile_WithDuplicateEmail_ShouldReturnEditViewWithError() throws Exception {
        // Arrange
        Long userId = 1L;
        when(userService.updateUser(any()))
                .thenThrow(new IllegalArgumentException("A user with this email already exists"));

        // Act & Assert
        mockMvc.perform(post("/profile/edit/" + userId)
                        .param("fullName", "Test User")
                        .param("email", "taken@test.com")
                        .with(authentication(authFor(userId, "Test User", "user@test.com", UserAuthorization.USER)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("Checking if POST /profile/edit/{id} returns 403 when submitted by an unauthorized user")
    void updateProfile_AsUnauthorizedUser_ShouldReturnForbidden() throws Exception {
        // Arrange
        Long targetUserId = 999L;

        doThrow(new UnauthorizedException("You do not have permission to edit this profile."))
                .when(userService).validateProfileAccess(any(UserPrincipal.class), eq(targetUserId));

        // Act & Assert
        mockMvc.perform(post("/profile/edit/" + targetUserId)
                        .param("fullName", "Hacked")
                        .param("email", "hacked@test.com")
                        .with(authentication(authFor(1L, "Test User", "user@test.com", UserAuthorization.USER)))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── GET /user/list ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if GET /user/list returns the user list view when accessed by a SYSADMIN")
    void userListView_AsSysAdmin_ShouldReturnListViewWithUsers() throws Exception {
        // Arrange
        Long sysAdminId = 1L;
        when(userService.findAll()).thenReturn(List.of(
                new UserDTO(1L, "SysAdmin", "sysadmin@test.com", UserAuthorization.SYSADMIN),
                new UserDTO(2L, "User", "user@test.com", UserAuthorization.USER)
        ));

        // Act & Assert
        mockMvc.perform(get("/user/list")
                        .with(authentication(authFor(sysAdminId, "SysAdmin", "sysadmin@test.com", UserAuthorization.SYSADMIN))))
                .andExpect(status().isOk())
                .andExpect(view().name("user/list"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("name"));
    }

    @Test
    @WithMockUser
    @DisplayName("Checking if GET /user/list returns 403 when accessed by a non-SYSADMIN")
    void userListView_AsNonSysAdmin_ShouldReturnForbidden() throws Exception {
        // Act & Assert — @PreAuthorize("hasRole('SYSADMIN')") rejects USER

        mockMvc.perform(get("/user/list")
                .with(authentication(authFor(1L, "Test User", "user@test.com", UserAuthorization.USER))))
                .andExpect(status().isForbidden());
    }

    // ── GET /dashboard/applicant ──────────────────────────────────────────────

    @Test
    @DisplayName("Checking if GET /dashboard/applicant returns the applicant dashboard with their visa list")
    void applicantDashboard_AsUser_ShouldReturnDashboardWithVisas() throws Exception {
        // Arrange
        Long userId = 1L;
        when(visaService.findVisasByApplicantId(userId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/dashboard/applicant")
                        .with(authentication(authFor(userId, "Test User", "user@test.com", UserAuthorization.USER))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/applicant"))
                .andExpect(model().attributeExists("visas"))
                .andExpect(model().attributeExists("name"));

        verify(visaService).findVisasByApplicantId(userId);
    }

    // ── GET /dashboard/admin ──────────────────────────────────────────────────

    @Test
    @DisplayName("Checking if GET /dashboard/admin returns assigned and unassigned cases for an ADMIN")
    void adminDashboard_AsAdmin_ShouldReturnDashboardWithCases() throws Exception {
        // Arrange
        Long adminId = 1L;
        when(visaService.findVisasByHandlerId(adminId)).thenReturn(List.of());
        when(visaService.findVisaByStatus("SUBMITTED")).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/dashboard/admin")
                        .with(authentication(authFor(adminId, "Test Admin", "admin@test.com", UserAuthorization.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/admin"))
                .andExpect(model().attributeExists("assignedCases"))
                .andExpect(model().attributeExists("unassignedCases"))
                .andExpect(model().attributeExists("name"));
    }

    @Test
    @DisplayName("Checking if GET /dashboard/admin returns 403 when accessed by a regular USER")
    void adminDashboard_AsRegularUser_ShouldReturnForbidden() throws Exception {
        // Act & Assert — @PreAuthorize("hasRole('ADMIN')") rejects USER
        mockMvc.perform(get("/dashboard/admin")
                        .with(authentication(authFor(1L, "Test User", "user@test.com", UserAuthorization.USER))))
                .andExpect(status().isForbidden());
    }

    // ── GET /dashboard/sysadmin ───────────────────────────────────────────────

    @Test
    @DisplayName("Checking if GET /dashboard/sysadmin returns users and audit logs for a SYSADMIN")
    void sysAdminDashboard_AsSysAdmin_ShouldReturnDashboardWithUsersAndLogs() throws Exception {
        // Arrange
        Long sysAdminId = 1L;
        when(userService.findAll()).thenReturn(List.of());
        when(auditService.findAll()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/dashboard/sysadmin")
                        .with(authentication(authFor(sysAdminId, "SysAdmin", "sysadmin@test.com", UserAuthorization.SYSADMIN))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/sysadmin"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("auditLogs"))
                .andExpect(model().attributeExists("name"));
    }

    @Test
    @DisplayName("Checking if GET /dashboard/sysadmin returns 403 when accessed by an ADMIN")
    void sysAdminDashboard_AsAdmin_ShouldReturnForbidden() throws Exception {
        // Act & Assert — @PreAuthorize("hasRole('SYSADMIN')") rejects ADMIN
        mockMvc.perform(get("/dashboard/sysadmin")
                        .with(authentication(authFor(1L, "Test Admin", "admin@test.com", UserAuthorization.ADMIN))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Checking if GET /dashboard/sysadmin returns 403 when accessed by a regular USER")
    void sysAdminDashboard_AsRegularUser_ShouldReturnForbidden() throws Exception {
        // Act & Assert — @PreAuthorize("hasRole('SYSADMIN')") rejects USER
        mockMvc.perform(get("/dashboard/sysadmin")
                        .with(authentication(authFor(1L, "Test User", "user@test.com", UserAuthorization.USER))))
                .andExpect(status().isForbidden());
    }

    // ── Helper methods ────────────────────────────────────────────────────────


    private Authentication authFor(Long id, String fullName, String email, UserAuthorization auth) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setUsername(email);
        user.setPassword("password");
        user.setUserAuthorization(auth);
        UserPrincipal principal = new UserPrincipal(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}