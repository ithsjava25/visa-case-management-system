package org.example.visacasemanagementsystem;

import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Objects;

@Controller
public class ApplicationViewController {

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return "redirect:/user/login";
        }
        boolean isSysAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_SYSADMIN"));
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));

        if (isSysAdmin) return "redirect:/dashboard/sysadmin";
        if (isAdmin)    return "redirect:/dashboard/admin";
        // Applicants use /visas/dashboard as their landing dashboard.
        return "redirect:/visas/dashboard";
    }
}
