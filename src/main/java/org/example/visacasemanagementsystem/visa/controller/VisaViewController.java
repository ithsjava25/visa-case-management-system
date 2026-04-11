package org.example.visacasemanagementsystem.visa.controller;

import jakarta.persistence.EntityNotFoundException;
import org.example.visacasemanagementsystem.comment.service.CommentService;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.service.UserService;
import org.example.visacasemanagementsystem.visa.VisaType;
import org.example.visacasemanagementsystem.visa.dto.CreateVisaDTO;
import org.example.visacasemanagementsystem.visa.dto.VisaDTO;
import org.example.visacasemanagementsystem.visa.service.VisaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    // ENDPOINTS

    // GET/visas/dashboard -> Huvudsidan. Visa listan på visum (alla för admin, egna för användaren)

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
        } else  {
            // Normal user/applicant can only se their own visa applications
            visas = visaService.findVisasByApplicant(currentUserId);
        }

        // Send data to Thymeleaf
        model.addAttribute("visas", visas);
        model.addAttribute("currentUser", user);

        return "visa/dashboard";

    }

    // GET/visas/{id} -> Detaljvyn. Visa all info om ett ärende + dess kommentarer
@GetMapping("/{id}")
    public String viewDetails(@PathVariable Long id, @RequestParam Long currentUserId , Model model) {
        // Get visa
        VisaDTO visa = visaService.findVisaDtoById(id);

        // Get comments
        var comments = commentService.getCommentsByVisaId(id);

        // Get user
        UserDTO user =  userService.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        model.addAttribute("visa", visa);
        model.addAttribute("comments", comments);
        model.addAttribute("currentUser", user);

        return "visa/details";
    }

    // GET/visas/apply -> Ansökningssidan. Visa formuläret för att söka ett nytt visum
    @GetMapping("/apply")
    public String showApplyForm(@RequestParam Long currentUserId, Model model) {
        UserDTO user = userService.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        model.addAttribute("currentUser", user);
        model.addAttribute("visaTypes", VisaType.values());
        return "visa/apply-form";
    }

    // POST/visas/apply -> Skicka ansökan. Tar emot förmulärdatan och sparar via VisaService
    @PostMapping("/apply")
    public String submitApplication(@ModelAttribute CreateVisaDTO createVisaDTO,
                                     @RequestParam Long currentUserId) {
        visaService.applyForVisa(createVisaDTO, currentUserId);
        return "redirect:/visas/dashboard?currentUserId=" + currentUserId;
    }

    // POST/visas/{id}/approve -> Beslut. (Admin) Godkänner visumet via en knapp i detaljvyn
    @PostMapping("/{id}/approve")
    public String approveVisa(@PathVariable Long id, @RequestParam Long currentUserId) {
       visaService.approveVisa(id, currentUserId);
       return "redirect:/visas/" + id + "?currentUserId=" + currentUserId;
    }

    // POST/visas/{id}/reject -> Beslut (Admin) Avvisar visumet med en motivering
    @PostMapping("/{id}/reject")
    public String rejectVisa(@PathVariable Long id,
                             @RequestParam Long currentUserId,
                             @RequestParam String reason) {
        visaService.rejectVisa(id, currentUserId, reason);
        return "redirect:/visas/" + id + "?currentUserId=" + currentUserId;
    }

    // ASSIGN - Admin tar på sig ett ärende
    @PostMapping("/{id}/assign")
    public String assignCaseToHandler(@PathVariable Long id, @RequestParam Long currentUserId) {
        visaService.assignHandler(id, currentUserId);
        return "redirect:/visas/" + id + "?currentUserId=" + currentUserId;
    }
}
