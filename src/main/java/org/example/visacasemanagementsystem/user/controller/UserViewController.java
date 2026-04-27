package org.example.visacasemanagementsystem.user.controller;

import jakarta.persistence.EntityNotFoundException;
import org.example.visacasemanagementsystem.audit.service.UserLogService;
import org.example.visacasemanagementsystem.audit.service.VisaLogService;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.dto.CreateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UpdateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.example.visacasemanagementsystem.user.service.UserService;
import org.example.visacasemanagementsystem.visa.dto.VisaDTO;
import org.example.visacasemanagementsystem.visa.service.VisaService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Controller
public class UserViewController {
    private final VisaService visaService;
    private final UserService userService;
    private final VisaLogService visaLogService;
    private final UserLogService userLogService;

    public UserViewController(VisaService visaService,
                              UserService userService,
                              VisaLogService visaLogService,
                              UserLogService userLogService) {
        this.visaService = visaService;
        this.userService = userService;
        this.visaLogService = visaLogService;
        this.userLogService = userLogService;
    }

    // Signup form for new users, accessible to all
    @GetMapping("/user/signup")
    public String userSignupForm(Model model) {
        return "user/signup";
    }

    // Posting a successful user creation then redirecting to the applicant dashboard since only applicants can be
    // created through the signup process. Admins or Sysadmins are created from applicant users by given authorization
    // from a sysadmin.
    @PostMapping("/user/signup")
    public String createUser(@RequestParam String fullName,
                             @RequestParam String email,
                             @RequestParam String password,
                             Model model) {
        try {
            CreateUserDTO dto = new CreateUserDTO(fullName, email, password, UserAuthorization.USER);
            userService.createUser(dto);
            return "redirect:/user/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "user/signup";
        }
    }

    // Login page
    @GetMapping("/user/login")
    public String userLoginForm(){
        return "user/login";
    }

    @GetMapping(value = "/static/google-icon.svg", produces = "image/svg+xml")
    public ResponseEntity<Resource> icon() throws IOException {
        String inputFile = "src/main/resources/static/google-icon.svg";
        Path path = new File(inputFile).toPath();
        FileSystemResource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(Files.probeContentType(path)))
                .body(resource);
    }

    // Uneditable profile view from where the user themselves or a sysadmin can access the profile edit view through a
    // button only available to them
    @GetMapping("/profile/view/{userId}")
    public String viewProfile(@AuthenticationPrincipal UserPrincipal principal,
                              @PathVariable Long userId,
                              Model model) {
        UserDTO user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        userService.validateProfileAccess(principal, userId);

        boolean isOwnProfile = principal.getUserId().equals(userId);
        boolean isSysAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_SYSADMIN"));

        model.addAttribute("user", user);
        model.addAttribute("canEdit", isOwnProfile || isSysAdmin);
        return "profile/view";
    }

    // Form for editing a users information, only available to the user themselves and sysadmins
    @GetMapping("/profile/edit/{userId}")
    public String showProfileEditForm(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long userId,
                                      Model model) {
        userService.validateProfileAccess(principal, userId);

        UserDTO user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        model.addAttribute("user", user);
        addAuthorizationFormAttributes(model, principal, userId);
        return "profile/edit";
    }

    // Posting information from user edit form
    @PostMapping("/profile/edit/{userId}")
    public String updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                @PathVariable Long userId,
                                @RequestParam String fullName,
                                @RequestParam String email,
                                Model model) {
        userService.validateProfileAccess(principal, userId);

        try {
            UpdateUserDTO dto = new UpdateUserDTO(userId, fullName, email);
            userService.updateUser(dto, principal.getUserId());
            return "redirect:/profile/view/" + userId;
        } catch (IllegalArgumentException e) {
            // Re-fetch the user so the (separate) authorization form on the same page
            // can keep showing the correct currently-selected role.
            UserDTO existing = userService.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", new UserDTO(userId, fullName, email, existing.userAuthorization()));
            addAuthorizationFormAttributes(model, principal, userId);
            return "profile/edit";
        }
    }

    // Sysadmin-only endpoint that backs the role dropdown
    @PostMapping("/profile/edit/{userId}/authorization")
    public String updateAuthorization(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long userId,
                                      @RequestParam UserAuthorization newAuthorization,
                                      Model model) {
        if (principal.getUserId().equals(userId)) {
            throw new UnauthorizedException("Sysadmins cannot change their own authorization.");
        }
        try {
            userService.updateUserAuthorization(principal.getUserId(), userId, newAuthorization);
            return "redirect:/profile/view/" + userId;
        } catch (IllegalArgumentException e) {
            UserDTO user = userService.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", user);
            addAuthorizationFormAttributes(model, principal, userId);
            return "profile/edit";
        }
    }

    // Adds the two model attributes the role dropdown on profile/edit needs
    private void addAuthorizationFormAttributes(Model model, UserPrincipal principal, Long userId) {
        boolean isOwnProfile = principal.getUserId().equals(userId);
        boolean isSysAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_SYSADMIN"));
        model.addAttribute("canChangeAuthorization", isSysAdmin && !isOwnProfile);
        model.addAttribute("authorizations", UserAuthorization.values());
    }

    // A list view of users only available to sysadmins
    @GetMapping("/user/list")
    public String userListView(@AuthenticationPrincipal UserPrincipal principal,
                               Model model) {
        List<UserDTO> allUsers = userService.findAll();
        model.addAttribute("name", principal.getFullName());
        model.addAttribute("users", allUsers);
        return "user/list";
    }

    @GetMapping("/dashboard/applicant")
    public String applicantDashboard(@AuthenticationPrincipal UserPrincipal principal,
                                     Model model) {
        List<VisaDTO> visas = visaService.findVisasByApplicantId(principal.getUserId());
        model.addAttribute("name", principal.getFullName());
        model.addAttribute("visas", visas);
        return "dashboard/applicant";
    }

    @GetMapping("/dashboard/admin")
    public String adminDashboard(@AuthenticationPrincipal UserPrincipal principal,
                                 Model model) {
        List<VisaDTO> assignedCases = visaService.findVisasByHandlerId(principal.getUserId());
        List<VisaDTO> unassignedCases = visaService.findVisaByStatus("SUBMITTED");
        model.addAttribute("name", principal.getFullName());
        model.addAttribute("assignedCases", assignedCases);
        model.addAttribute("unassignedCases", unassignedCases);
        return "dashboard/admin";
    }

    @GetMapping("/dashboard/sysadmin")
    public String sysAdminDashboard(@AuthenticationPrincipal UserPrincipal principal,
                                    Model model) {
        List<UserDTO> allUsers = userService.findAll();
        model.addAttribute("name", principal.getFullName());
        model.addAttribute("users", allUsers);
        model.addAttribute("visaLogs", visaLogService.findAll());
        model.addAttribute("userLogs", userLogService.findAll());
        return "dashboard/sysadmin";
    }
}