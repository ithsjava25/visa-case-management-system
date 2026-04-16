package org.example.visacasemanagementsystem.user.controller;

import org.example.visacasemanagementsystem.TestcontainersConfiguration;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.user.security.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;
    private User savedAdmin;
    private User savedSysAdmin;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setFullName("Test User");
        user.setEmail("user@controller.test");
        user.setPassword("password123");
        user.setUserAuthorization(UserAuthorization.USER);
        savedUser = userRepository.saveAndFlush(user);

        User admin = new User();
        admin.setFullName("Test Admin");
        admin.setEmail("admin@controller.test");
        admin.setPassword("password123");
        admin.setUserAuthorization(UserAuthorization.ADMIN);
        savedAdmin = userRepository.saveAndFlush(admin);

        User sysadmin = new User();
        sysadmin.setFullName("Test SysAdmin");
        sysadmin.setEmail("sysadmin@controller.test");
        sysadmin.setPassword("password123");
        sysadmin.setUserAuthorization(UserAuthorization.SYSADMIN);
        savedSysAdmin = userRepository.saveAndFlush(sysadmin);
    }

    /** Build a fully populated {@link Authentication} from a persisted {@link User}. */
    private Authentication authFor(User user) {
        SecurityUser principal = new SecurityUser(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    // ── GET /user/signup ──────────────────────────────────────────────────────

    @Test
    void userSignupForm_ReturnsSignupView() throws Exception {
        mockMvc.perform(get("/user/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/signup"));
    }

    // ── POST /user/signup ─────────────────────────────────────────────────────

    @Test
    void createUser_WithValidData_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/user/signup")
                        .param("fullName", "New Applicant")
                        .param("email", "newapplicant@controller.test")
                        .param("password", "securePass1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/login"));
    }

    @Test
    void createUser_WithDuplicateEmail_ReturnsSignupViewWithError() throws Exception {
        mockMvc.perform(post("/user/signup")
                        .param("fullName", "Duplicate User")
                        .param("email", "user@controller.test")   // already seeded
                        .param("password", "password123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("user/signup"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void createUser_WithShortPassword_ReturnsSignupViewWithError() throws Exception {
        mockMvc.perform(post("/user/signup")
                        .param("fullName", "Short Pass")
                        .param("email", "shortpass@controller.test")
                        .param("password", "abc")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("user/signup"))
                .andExpect(model().attributeExists("error"));
    }

    // ── GET /user/login ───────────────────────────────────────────────────────

    @Test
    void userLoginForm_ReturnsLoginView() throws Exception {
        mockMvc.perform(get("/user/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/login"));
    }

    // ── GET /profile/view/{userId} ────────────────────────────────────────────

    @Test
    void viewProfile_AsOwnUser_ReturnsProfileViewWithCanEditTrue() throws Exception {
        mockMvc.perform(get("/profile/view/" + savedUser.getId())
                        .with(authentication(authFor(savedUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/view"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("canEdit", true));
    }

    @Test
    void viewProfile_AsSysAdminViewingOtherUser_ReturnsProfileViewWithCanEditTrue() throws Exception {
        mockMvc.perform(get("/profile/view/" + savedUser.getId())
                        .with(authentication(authFor(savedSysAdmin))))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/view"))
                .andExpect(model().attribute("canEdit", true));
    }

    @Test
    void viewProfile_AsAdminViewingOtherUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/profile/view/" + savedSysAdmin.getId())
                        .with(authentication(authFor(savedAdmin))))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewProfile_AsUserViewingOtherProfile_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/profile/view/" + savedAdmin.getId())
                        .with(authentication(authFor(savedUser))))
                .andExpect(status().isForbidden());
    }

    // ── GET /profile/edit/{userId} ────────────────────────────────────────────

    @Test
    void showProfileEditForm_AsOwnUser_ReturnsEditViewWithUserModel() throws Exception {
        mockMvc.perform(get("/profile/edit/" + savedUser.getId())
                        .with(authentication(authFor(savedUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void showProfileEditForm_AsSysAdmin_ReturnsEditView() throws Exception {
        mockMvc.perform(get("/profile/edit/" + savedUser.getId())
                        .with(authentication(authFor(savedSysAdmin))))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"));
    }

    @Test
    void showProfileEditForm_AsUnauthorizedUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/profile/edit/" + savedSysAdmin.getId())
                        .with(authentication(authFor(savedUser))))
                .andExpect(status().isForbidden());
    }

    // ── POST /profile/edit/{userId} ───────────────────────────────────────────

    @Test
    void updateProfile_WithValidData_RedirectsToProfileView() throws Exception {
        mockMvc.perform(post("/profile/edit/" + savedUser.getId())
                        .param("fullName", "Updated Name")
                        .param("email", "updatedname@controller.test")
                        .with(authentication(authFor(savedUser)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/view/" + savedUser.getId()));
    }

    @Test
    void updateProfile_WithDuplicateEmail_ReturnsEditViewWithError() throws Exception {
        mockMvc.perform(post("/profile/edit/" + savedUser.getId())
                        .param("fullName", "Test User")
                        .param("email", "admin@controller.test")   // already owned by savedAdmin
                        .with(authentication(authFor(savedUser)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void updateProfile_AsUnauthorizedUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/profile/edit/" + savedAdmin.getId())
                        .param("fullName", "Hacked Name")
                        .param("email", "hacked@controller.test")
                        .with(authentication(authFor(savedUser)))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── GET /user/list ────────────────────────────────────────────────────────

    @Test
    void userListView_AsSysAdmin_ReturnsListViewWithUsers() throws Exception {
        mockMvc.perform(get("/user/list")
                        .with(authentication(authFor(savedSysAdmin))))
                .andExpect(status().isOk())
                .andExpect(view().name("user/list"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("name"));
    }

    @Test
    void userListView_AsNonSysAdmin_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/user/list")
                        .with(authentication(authFor(savedUser))))
                .andExpect(status().isForbidden());
    }

    // ── GET /dashboard/applicant ──────────────────────────────────────────────

    @Test
    void applicantDashboard_AsUser_ReturnsDashboardViewWithVisas() throws Exception {
        mockMvc.perform(get("/dashboard/applicant")
                        .with(authentication(authFor(savedUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/applicant"))
                .andExpect(model().attributeExists("visas"))
                .andExpect(model().attributeExists("name"));
    }

    // ── GET /dashboard/admin ──────────────────────────────────────────────────

    @Test
    void adminDashboard_AsAdmin_ReturnsDashboardViewWithCases() throws Exception {
        mockMvc.perform(get("/dashboard/admin")
                        .with(authentication(authFor(savedAdmin))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/admin"))
                .andExpect(model().attributeExists("assignedCases"))
                .andExpect(model().attributeExists("unassignedCases"))
                .andExpect(model().attributeExists("name"));
    }

    @Test
    void adminDashboard_AsSysAdmin_ReturnsDashboardView() throws Exception {
        mockMvc.perform(get("/dashboard/admin")
                        .with(authentication(authFor(savedSysAdmin))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/admin"));
    }

    @Test
    void adminDashboard_AsRegularUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/dashboard/admin")
                        .with(authentication(authFor(savedUser))))
                .andExpect(status().isForbidden());
    }

    // ── GET /dashboard/sysadmin ───────────────────────────────────────────────

    @Test
    void sysAdminDashboard_AsSysAdmin_ReturnsDashboardViewWithUsersAndAuditLogs() throws Exception {
        mockMvc.perform(get("/dashboard/sysadmin")
                        .with(authentication(authFor(savedSysAdmin))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/sysadmin"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("auditLogs"))
                .andExpect(model().attributeExists("name"));
    }

    @Test
    void sysAdminDashboard_AsAdmin_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/dashboard/sysadmin")
                        .with(authentication(authFor(savedAdmin))))
                .andExpect(status().isForbidden());
    }

    @Test
    void sysAdminDashboard_AsRegularUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/dashboard/sysadmin")
                        .with(authentication(authFor(savedUser))))
                .andExpect(status().isForbidden());
    }
}
