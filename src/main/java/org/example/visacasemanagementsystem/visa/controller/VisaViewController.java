package org.example.visacasemanagementsystem.visa.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.example.visacasemanagementsystem.comment.service.CommentService;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.file.FileService;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.example.visacasemanagementsystem.user.service.UserService;
import org.example.visacasemanagementsystem.visa.VisaType;
import org.example.visacasemanagementsystem.visa.dto.CreateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.UpdateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.VisaDTO;
import org.example.visacasemanagementsystem.visa.service.VisaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@PreAuthorize("isAuthenticated()")
@Controller
@RequestMapping("/visas")
public class VisaViewController {

    private final VisaService visaService;
    private final CommentService commentService;
    private final UserService userService;
    private final FileService fileService;

    public VisaViewController(VisaService visaService, CommentService commentService, UserService userService,  FileService fileService) {
        this.visaService = visaService;
        this.commentService = commentService;
        this.userService = userService;
        this.fileService = fileService;
    }

    @GetMapping("/dashboard")
    public String showDashboard(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        UserDTO user = userService.findById(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found."));
        List<VisaDTO> visas;
        // If the current user = ADMIN/SYSADMIN show everything
        if (  user.userAuthorization() == UserAuthorization.ADMIN ||
                user.userAuthorization() == UserAuthorization.SYSADMIN) {
            visas = visaService.findAll();
        } else {
            // Normal user/applicant can only se their own visa applications
            visas = visaService.findVisasByApplicant(principal.getUserId());
        }

        // Send data to Thymeleaf
        model.addAttribute("visas", visas);
        model.addAttribute("currentUser", user);

        return "visa/dashboard";
    }

    @GetMapping("/apply")
    public String showApplyForm(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        UserDTO user = userService.findById(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        model.addAttribute("currentUser", user);
        model.addAttribute("visaTypes", VisaType.values());

        if (!model.containsAttribute("createVisaDTO")) {
            model.addAttribute("createVisaDTO",
                    new CreateVisaDTO(null,
                            "",
                            "",
                            null,
                            principal.getUserId()));
        }

        return "visa/apply-form";
    }

    @PostMapping("/apply")
    public String submitApplication(
            @Valid @ModelAttribute("createVisaDTO") CreateVisaDTO createVisaDTO,
            BindingResult bindingResult,
            @RequestParam(value = "passportFile",  required = false) MultipartFile passportFile,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {
        
        if (bindingResult.hasErrors()) {
            prepareApplyModel(principal.getUserId(), model);
            return "visa/apply-form";
        }

        try {
            String s3Key = null;

            if (passportFile != null && !passportFile.isEmpty()) {
                s3Key = fileService.uploadFile(passportFile);
            }

            visaService.applyForVisa(createVisaDTO, principal.getUserId(), s3Key);

        } catch (java.io.IOException e) {
            bindingResult.reject("upload.error", "Failed to upload document: " + e.getMessage());
            prepareApplyModel(principal.getUserId(), model);

            return "visa/apply-form";

        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("travelDate", "error.travelDate", e.getMessage());
            prepareApplyModel(principal.getUserId(), model);
            return "visa/apply-form";
        }

        return "redirect:/visas/dashboard";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        UserDTO userDTO  = userService.findById(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        VisaDTO visa = visaService.findVisaDtoById(id);

        if (!visa.applicantId().equals(principal.getUserId())) {
            throw  new UnauthorizedException("You can only edit your own applications.");
        }

        UpdateVisaDTO updateDto = new UpdateVisaDTO(
                visa.id(),
                visa.visaType(),
                visa.visaStatus(),
                visa.nationality(),
                visa.passportNumber(),
                visa.travelDate(),
                visa.handlerId(),
                visa.statusInformation()
        );

        model.addAttribute("updateVisaDto", updateDto);
        model.addAttribute("visa", visa);
        model.addAttribute("currentUser", userDTO);
        model.addAttribute("visaTypes", VisaType.values());
        model.addAttribute("isEdit", true);
        model.addAttribute("statusInformation", visa.statusInformation());

        return "visa/edit-form";
    }

    @PostMapping("/{id}/edit")
    public String processUpdate(
            @PathVariable Long id,
            @Valid @ModelAttribute("updateVisaDto") UpdateVisaDTO updateVisaDTO,
            BindingResult bindingResult,
            @RequestParam(value = "passportFile", required = false) MultipartFile passportFile,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {

        if (bindingResult.hasErrors()) {
            VisaDTO visa = visaService.findVisaDtoById(id);
            model.addAttribute("visa", visa);
            prepareApplyModel(principal.getUserId(), model);
            model.addAttribute("isEdit", true);
            model.addAttribute("statusInformation", visa.statusInformation());
            return "visa/edit-form";
        }

        try {
            String newS3Key = null;

            if (passportFile != null && !passportFile.isEmpty()) {
                newS3Key = fileService.uploadFile(passportFile);
            }

            visaService.updateVisa(id, updateVisaDTO, principal.getUserId(), newS3Key);

        } catch (java.io.IOException e) {
            bindingResult.reject("upload.error", "Failed to upload new document: " + e.getMessage());
            VisaDTO visa  = visaService.findVisaDtoById(id);
            model.addAttribute("visa", visa);
            prepareApplyModel(principal.getUserId(), model);
            model.addAttribute("isEdit", true);
            return "visa/edit-form";

        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("date")) {
                bindingResult.rejectValue("travelDate", "error.travelDate", e.getMessage());
            } else {
                bindingResult.reject("globalError", e.getMessage());
            }

            prepareApplyModel(principal.getUserId(), model);
            model.addAttribute("isEdit", true);
            VisaDTO visa = visaService.findVisaDtoById(id);
            model.addAttribute("visa", visa);
            model.addAttribute("statusInformation", visa.statusInformation());

            return "visa/edit-form";
        }

        return "redirect:/visas/" + id;
    }

    @PostMapping("/{id}/documents/delete")
    public String deleteVisaDocument(@PathVariable Long id,
                                     @RequestParam String s3Key,
                                     @AuthenticationPrincipal UserPrincipal principal) {

        visaService.removeVisaDocument(id, s3Key, principal.getUserId());
        return "redirect:/visas/" + id + "/edit";

    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/approve")
    public String approveVisa(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        visaService.approveVisa(id, principal.getUserId());
        return "redirect:/visas/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/request-info")
    public String requestMoreInformation(@PathVariable Long id,
                                         @AuthenticationPrincipal UserPrincipal principal,
                                         @RequestParam String reason) {

        visaService.requestMoreInformation(id, principal.getUserId(), reason);

        return "redirect:/visas/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reject")
    public String rejectVisa(@PathVariable Long id,
                             @AuthenticationPrincipal UserPrincipal principal,
                             @RequestParam String reason) {
        visaService.rejectVisa(id, principal.getUserId(), reason);
        return "redirect:/visas/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/assign")
    public String assignCaseToHandler(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        visaService.assignHandler(id, principal.getUserId());
        return "redirect:/visas/" + id;
    }

    @GetMapping("/{id}")
    public String viewDetails(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        // Get visa
        VisaDTO visa = visaService.findVisaDtoById(id);

        // Get user
        UserDTO user = userService.findById(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        // Authorization check: regular users can only view their own applications
        if (user.userAuthorization() == UserAuthorization.USER && !visa.applicantId().equals(principal.getUserId())) {
            throw new UnauthorizedException("You can only view your own applications.");
        }

        // Get comments
        var comments = commentService.getCommentsByVisaId(id);

        model.addAttribute("visa", visa);
        model.addAttribute("comments", comments);
        model.addAttribute("currentUser", user);

        return "visa/details";
    }

    // Helper methods

    public void prepareApplyModel(Long currentUserId, Model model) {
        UserDTO user = userService.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));
        model.addAttribute("currentUser", user);
        model.addAttribute("visaTypes", VisaType.values());
    }
}
