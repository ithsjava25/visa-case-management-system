package org.example.visacasemanagementsystem.user.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.dto.CreateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UpdateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.example.visacasemanagementsystem.user.service.UserService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

/**
 * User-facing controller: signup, login, profile view/edit, and the sysadmin user list.
 */
@Controller
public class UserViewController {
    private final UserService userService;

    public UserViewController(UserService userService) {
        this.userService = userService;
    }

    // Signup form for new users, accessible to all
    @GetMapping("/user/signup")
    public String userSignupForm(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        if (principal != null) {
            return "redirect:/home";
        }
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
    public String userLoginForm(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null) {
            return "redirect:/home";
        }
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

    // Sign-out endpoint.
    @PostMapping("/user/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, auth);

        // Invalidate the HTTP session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/user/login?logout";
    }

    // Uneditable profile view
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

    // A list view of users only available to sysadmins
    @GetMapping("/user/list")
    public String userListView(@AuthenticationPrincipal UserPrincipal principal,
                               Model model) {
        List<UserDTO> allUsers = userService.findAll();
        model.addAttribute("name", principal.getFullName());
        model.addAttribute("users", allUsers);
        return "user/list";
    }

    // Adds the two model attributes the role dropdown on profile/edit needs
    private void addAuthorizationFormAttributes(Model model, UserPrincipal principal, Long userId) {
        boolean isOwnProfile = principal.getUserId().equals(userId);
        boolean isSysAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_SYSADMIN"));
        model.addAttribute("canChangeAuthorization", isSysAdmin && !isOwnProfile);
        model.addAttribute("authorizations", UserAuthorization.values());
    }
}