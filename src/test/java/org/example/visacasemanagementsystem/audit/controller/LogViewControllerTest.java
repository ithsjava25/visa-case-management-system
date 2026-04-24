package org.example.visacasemanagementsystem.audit.controller;

import org.example.visacasemanagementsystem.audit.UserEventType;
import org.example.visacasemanagementsystem.audit.VisaEventType;
import org.example.visacasemanagementsystem.audit.dto.UserLogDTO;
import org.example.visacasemanagementsystem.audit.dto.VisaLogDTO;
import org.example.visacasemanagementsystem.audit.service.UserLogService;
import org.example.visacasemanagementsystem.audit.service.VisaLogService;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Web-layer tests for {@link LogViewController}.
 *
 * These tests use @WebMvcTest without importing SecurityConfig, which matches
 * the pattern used by the other view-controller tests in this project. The
 * URL-level SYSADMIN-only rule for /log/** lives in SecurityConfig and is
 * covered by the integration tests; what we verify here is that, given an
 * authenticated SYSADMIN, the controller wires the right filters, default
 * paging, and enum lists into the view.
 *
 * Important: MockMvc actually renders the Thymeleaf view, and the shared
 * fragments/header.html reads ${#authentication.principal.userId}. That
 * property only exists on our own UserPrincipal, not on Spring Security's
 * default @WithMockUser principal, so every test installs a real
 * UserPrincipal into SecurityContextHolder via {@link #loginAsSysadmin()}.
 * Without that, Thymeleaf blows up rendering the header and the endpoint
 * 500s, which is exactly what the previous revision of these tests tripped
 * over.
 */
@WebMvcTest(LogViewController.class)
class LogViewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private VisaLogService visaLogService;

    @MockitoBean
    private UserLogService userLogService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Install a real {@link UserPrincipal} (not Spring Security's default
     * {@code User}) into the SecurityContext so the header fragment can read
     * {@code principal.userId} during view rendering.
     */
    private void loginAsSysadmin() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("sysadmin@test.com");
        testUser.setEmail("sysadmin@test.com");
        testUser.setPassword("password");
        testUser.setUserAuthorization(UserAuthorization.SYSADMIN);
        UserPrincipal principal = new UserPrincipal(testUser);
        Authentication auth = new TestingAuthenticationToken(
                principal, "password", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ── /log/visa ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "SYSADMIN")
    void visaLog_AsSysadmin_NoFilters_ShouldReturnPageWithDefaults() throws Exception {
        // Arrange — service returns an empty page; we only care about wiring here.
        loginAsSysadmin();
        Page<VisaLogDTO> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(visaLogService.findFiltered(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(empty);

        // Act & Assert
        mockMvc.perform(get("/log/visa"))
                .andExpect(status().isOk())
                .andExpect(view().name("log/visa"))
                .andExpect(model().attributeExists("logs"))
                .andExpect(model().attribute("eventTypes", VisaEventType.values()))
                .andExpect(model().attribute("selectedEventType", (Object) null))
                .andExpect(model().attribute("from", (Object) null))
                .andExpect(model().attribute("to", (Object) null));

        // The controller should apply: page 0, size 20, sort timeStamp DESC.
        Pageable expected = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timeStamp"));
        verify(visaLogService).findFiltered(isNull(), isNull(), isNull(), eq(expected));
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    void visaLog_AsSysadmin_WithFilters_ShouldPassThroughToService() throws Exception {
        // Arrange
        loginAsSysadmin();
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to   = LocalDate.of(2026, 4, 24);
        LocalDateTime expectedFrom = from.atStartOfDay();
        LocalDateTime expectedTo   = to.atTime(LocalTime.MAX);

        VisaLogDTO sample = new VisaLogDTO(
                1L, LocalDateTime.now(), 42L, 101L, VisaEventType.GRANTED, "Granted.");
        Page<VisaLogDTO> page = new PageImpl<>(List.of(sample), PageRequest.of(0, 20), 1);
        when(visaLogService.findFiltered(
                eq(VisaEventType.GRANTED), eq(expectedFrom), eq(expectedTo), any(Pageable.class)))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/log/visa")
                        .param("eventType", "GRANTED")
                        .param("from", "2026-04-01")
                        .param("to",   "2026-04-24"))
                .andExpect(status().isOk())
                .andExpect(view().name("log/visa"))
                .andExpect(model().attribute("selectedEventType", VisaEventType.GRANTED))
                .andExpect(model().attribute("from", from))
                .andExpect(model().attribute("to", to));

        verify(visaLogService).findFiltered(
                eq(VisaEventType.GRANTED),
                eq(expectedFrom),
                eq(expectedTo),
                any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    void visaLog_AsSysadmin_WithExplicitPageAndSize_ShouldUseThem() throws Exception {
        // Arrange
        loginAsSysadmin();
        Page<VisaLogDTO> empty = new PageImpl<>(List.of(), PageRequest.of(2, 50), 0);
        when(visaLogService.findFiltered(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(empty);

        // Act & Assert
        mockMvc.perform(get("/log/visa")
                        .param("page", "2")
                        .param("size", "50"))
                .andExpect(status().isOk());

        Pageable expected = PageRequest.of(2, 50, Sort.by(Sort.Direction.DESC, "timeStamp"));
        verify(visaLogService).findFiltered(isNull(), isNull(), isNull(), eq(expected));
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    void visaLog_AsSysadmin_WithOversizedPageSize_ShouldClampToMax() throws Exception {
        // Arrange — size=1000 should be clamped to MAX_PAGE_SIZE (100).
        loginAsSysadmin();
        Page<VisaLogDTO> empty = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
        when(visaLogService.findFiltered(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(empty);

        // Act & Assert
        mockMvc.perform(get("/log/visa").param("size", "1000"))
                .andExpect(status().isOk());

        Pageable expected = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "timeStamp"));
        verify(visaLogService).findFiltered(isNull(), isNull(), isNull(), eq(expected));
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    void visaLog_AsSysadmin_WithNegativePageAndSize_ShouldClampToSafeMinimums() throws Exception {
        // Arrange — page=-5 → 0, size=0 → 1.
        loginAsSysadmin();
        Page<VisaLogDTO> empty = new PageImpl<>(List.of(), PageRequest.of(0, 1), 0);
        when(visaLogService.findFiltered(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(empty);

        // Act & Assert
        mockMvc.perform(get("/log/visa")
                        .param("page", "-5")
                        .param("size", "0"))
                .andExpect(status().isOk());

        Pageable expected = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "timeStamp"));
        verify(visaLogService).findFiltered(isNull(), isNull(), isNull(), eq(expected));
    }

    // Note on negative auth coverage:
    // The SYSADMIN-only rule for /log/** is enforced URL-side by SecurityConfig
    // (`/log/** → hasRole("SYSADMIN")`), which is exercised by the full-context
    // integration tests. The @PreAuthorize on LogViewController is a
    // belt-and-suspenders duplicate. @WebMvcTest does not load
    // @EnableMethodSecurity unless SecurityConfig is imported, so adding a
    // negative assertion here would test nothing (or silently return 200 OK).

    // ── /log/user ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "SYSADMIN")
    void userLog_AsSysadmin_NoFilters_ShouldReturnPageWithDefaults() throws Exception {
        // Arrange
        loginAsSysadmin();
        Page<UserLogDTO> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(userLogService.findFiltered(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(empty);

        // Act & Assert
        mockMvc.perform(get("/log/user"))
                .andExpect(status().isOk())
                .andExpect(view().name("log/user"))
                .andExpect(model().attributeExists("logs"))
                .andExpect(model().attribute("eventTypes", UserEventType.values()))
                .andExpect(model().attribute("selectedEventType", (Object) null))
                .andExpect(model().attribute("from", (Object) null))
                .andExpect(model().attribute("to", (Object) null));

        Pageable expected = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timeStamp"));
        verify(userLogService).findFiltered(isNull(), isNull(), isNull(), eq(expected));
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    void userLog_AsSysadmin_WithFilters_ShouldPassThroughToService() throws Exception {
        // Arrange
        loginAsSysadmin();
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to   = LocalDate.of(2026, 1, 31);
        LocalDateTime expectedFrom = from.atStartOfDay();
        LocalDateTime expectedTo   = to.atTime(LocalTime.MAX);

        UserLogDTO sample = new UserLogDTO(
                1L, LocalDateTime.now(), 1L, 5L,
                UserEventType.AUTHORIZATION_CHANGED, "USER -> ADMIN");
        Page<UserLogDTO> page = new PageImpl<>(List.of(sample), PageRequest.of(0, 20), 1);
        when(userLogService.findFiltered(
                eq(UserEventType.AUTHORIZATION_CHANGED),
                eq(expectedFrom), eq(expectedTo), any(Pageable.class)))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/log/user")
                        .param("eventType", "AUTHORIZATION_CHANGED")
                        .param("from", "2026-01-01")
                        .param("to",   "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("log/user"))
                .andExpect(model().attribute("selectedEventType", UserEventType.AUTHORIZATION_CHANGED))
                .andExpect(model().attribute("from", from))
                .andExpect(model().attribute("to", to));

        verify(userLogService).findFiltered(
                eq(UserEventType.AUTHORIZATION_CHANGED),
                eq(expectedFrom),
                eq(expectedTo),
                any(Pageable.class));
    }

}
