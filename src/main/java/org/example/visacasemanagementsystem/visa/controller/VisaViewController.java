package org.example.visacasemanagementsystem.visa.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.example.visacasemanagementsystem.comment.service.CommentService;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.service.UserService;
import org.example.visacasemanagementsystem.visa.VisaType;
import org.example.visacasemanagementsystem.visa.dto.CreateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.UpdateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.VisaDTO;
import org.example.visacasemanagementsystem.visa.service.VisaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/visas")
public class VisaViewController {

    private final VisaService visaService;
    private final CommentService commentService;
    private final UserService userService;

    public VisaViewController(VisaService visaService, CommentService commentService, UserService userService) {
        this.visaService = visaService;
        this.commentService = commentService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String showDashboard(@RequestParam Long currentUserId, Model model) {
        // Find current user
        UserDTO user = userService.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        List<VisaDTO> visas;

        // If the current user = ADMIN/SYSADMIN show everything
        if (user.userAuthorization() == UserAuthorization.ADMIN ||
                user.userAuthorization() == UserAuthorization.SYSADMIN) {
            visas = visaService.findAll();
        } else {
            // Normal user/applicant can only se their own visa applications
            visas = visaService.findVisasByApplicant(currentUserId);
        }

        // Send data to Thymeleaf
        model.addAttribute("visas", visas);
        model.addAttribute("currentUser", user);

        return "visa/dashboard";
    }

    @GetMapping("/apply")
    public String showApplyForm(@RequestParam Long currentUserId, Model model) {
        UserDTO user = userService.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        model.addAttribute("currentUser", user);
        model.addAttribute("visaTypes", VisaType.values());

        if (!model.containsAttribute("createVisaDTO")) {
            model.addAttribute("createVisaDTO", new CreateVisaDTO(null, "", "", null, currentUserId));
        }

        return "visa/apply-form";
    }

    @PostMapping("/apply")
    public String submitApplication(
            @Valid @ModelAttribute("createVisaDTO") CreateVisaDTO createVisaDTO,
            BindingResult bindingResult,
            @RequestParam Long currentUserId,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            prepareApplyModel(currentUserId, model);
            return "visa/apply-form";
        }

        try {
            visaService.applyForVisa(createVisaDTO, currentUserId);
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("travelDate", "error.travelDate", e.getMessage());
            prepareApplyModel(currentUserId, model);
            return "visa/apply-form";
        }

        return "redirect:/visas/dashboard?currentUserId=" + currentUserId;
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, @RequestParam Long currentUserId, Model model) {
        UserDTO userDTO  = userService.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        VisaDTO visa = visaService.findVisaDtoById(id);

        if (!visa.applicantId().equals(currentUserId)) {
            throw  new UnauthorizedException("You can only edit your own applications.");
        }

        UpdateVisaDTO updateDto = new UpdateVisaDTO(
                visa.id(),
                visa.visaType(),
                visa.visaStatus(),
                visa.nationality(),
                visa.passportNumber(),
                visa.travelDate(),
                visa.handlerId()
        );

        model.addAttribute("updateVisaDto", updateDto);
        model.addAttribute("currentUser", userDTO);
        model.addAttribute("visaTypes", VisaType.values());
        model.addAttribute("isEdit", true);
        model.addAttribute("statusInformation", visa.statusInformation());

        return "visa/edit-form";
    }

    @PostMapping("/{id}/edit")
    public String processUpdate(
            @PathVariable Long id,
            @Valid @ModelAttribute ("updateVisaDto") UpdateVisaDTO updateVisaDTO,
            BindingResult bindingResult,
            @RequestParam Long currentUserId,
            Model model) {

        if (bindingResult.hasErrors()) {
            prepareApplyModel(currentUserId, model);
            model.addAttribute("isEdit", true);
            VisaDTO visa = visaService.findVisaDtoById(id);
            model.addAttribute("statusInformation", visa.statusInformation());
            return "visa/edit-form";
        }

        try {
            visaService.updateVisa(id, updateVisaDTO, currentUserId);
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("travelDate", "error.travelDate", e.getMessage());
            prepareApplyModel(currentUserId, model);
            model.addAttribute("isEdit", true);
            return "visa/edit-form";
        }
        return "redirect:/visas/" + id + "?currentUserId=" + currentUserId;
    }

    @PostMapping("/{id}/approve")
    public String approveVisa(@PathVariable Long id, @RequestParam Long currentUserId) {
        visaService.approveVisa(id, currentUserId);
        return "redirect:/visas/" + id + "?currentUserId=" + currentUserId;
    }

    @PostMapping("/{id}/request-info")
    public String requestMoreInformation(@PathVariable Long id,
                                         @RequestParam Long currentUserId,
                                         @RequestParam String reason) {

        visaService.requestMoreInformation(id, currentUserId, reason);

        return "redirect:/visas/" + id + "?currentUserId=" + currentUserId;
    }


    @PostMapping("/{id}/reject")
    public String rejectVisa(@PathVariable Long id,
                             @RequestParam Long currentUserId,
                             @RequestParam String reason) {
        visaService.rejectVisa(id, currentUserId, reason);
        return "redirect:/visas/" + id + "?currentUserId=" + currentUserId;
    }


    @PostMapping("/{id}/assign")
    public String assignCaseToHandler(@PathVariable Long id, @RequestParam Long currentUserId) {
        visaService.assignHandler(id, currentUserId);
        return "redirect:/visas/" + id + "?currentUserId=" + currentUserId;
    }

    @GetMapping("/{id}")
    public String viewDetails(@PathVariable Long id, @RequestParam Long currentUserId, Model model) {
        // Get visa
        VisaDTO visa = visaService.findVisaDtoById(id);

        // Get user
        UserDTO user = userService.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        // Authorization check: regular users can only view their own applications
        if (user.userAuthorization() == UserAuthorization.USER && !visa.applicantId().equals(currentUserId)) {
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
