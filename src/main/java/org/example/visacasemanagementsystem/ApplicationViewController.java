package org.example.visacasemanagementsystem;

import org.example.visacasemanagementsystem.audit.dto.AuditDTO;
import org.example.visacasemanagementsystem.audit.service.AuditService;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.security.SecurityUser;
import org.example.visacasemanagementsystem.user.service.UserService;
import org.example.visacasemanagementsystem.visa.dto.VisaDTO;
import org.example.visacasemanagementsystem.visa.service.VisaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ApplicationViewController {
    private final VisaService visaService;
    private final UserService userService;
    private final AuditService auditService;

    public ApplicationViewController(VisaService visaService,
                              UserService userService,
                              AuditService auditService) {
        this.visaService = visaService;
        this.userService = userService;
        this.auditService = auditService;
    }

    @GetMapping("/applicant/dashboard")
    public String applicantDashboard(@AuthenticationPrincipal SecurityUser principal,
                                     Model model) {
        List<VisaDTO> visas = visaService.findByApplicantId(principal.getUserId());
        model.addAttribute("name", principal.getFullName());
        model.addAttribute("visas", visas);
        return "dashboard/applicant";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(@AuthenticationPrincipal SecurityUser principal,
                                 Model model) {
        List<VisaDTO> assignedCases = visaService.findByHandlerId(principal.getUserId());
        List<VisaDTO> unassignedCases = visaService.findVisaByStatus("UNASSIGNED");
        model.addAttribute("name", principal.getFullName());
        model.addAttribute("assignedCases", assignedCases);
        model.addAttribute("unassignedCases", unassignedCases);
        return "dashboard/admin";
    }

    @GetMapping("/sysadmin/dashboard")
    public String sysAdminDashboard(@AuthenticationPrincipal SecurityUser principal,
                                    Model model) {
        List<UserDTO> allUsers = userService.findAll();
        List<AuditDTO> recentLogs = auditService.findAll();
        model.addAttribute("name", principal.getFullName());
        model.addAttribute("users", allUsers);
        model.addAttribute("auditLogs", recentLogs);
        return "dashboard/sysadmin";
    }
}
