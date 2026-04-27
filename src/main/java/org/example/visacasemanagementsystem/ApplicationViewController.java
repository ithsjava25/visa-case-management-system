package org.example.visacasemanagementsystem;

import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Objects;

/**
 * Entry-point controller: maps the site root to the role-based router and
 * hosts that router at /home.
 */
@Controller
public class ApplicationViewController {

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    /**
     * Role-based landing-page router. Each role has a distinct primary page:
     *   SYSADMIN -> /log/visa
     *   ADMIN    -> /visa/cases
     *   USER     -> /visa/my-applications.
     */
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return "redirect:/user/login";
        }

        boolean isSysAdmin = false;
        boolean isAdmin = false;
        for (var authority : principal.getAuthorities()) {
            String role = authority.getAuthority();
            if (Objects.equals(role, "ROLE_SYSADMIN")) isSysAdmin = true;
            else if (Objects.equals(role, "ROLE_ADMIN")) isAdmin = true;
        }

        if (isSysAdmin) return "redirect:/log/visa";
        if (isAdmin)    return "redirect:/visa/cases";
        return "redirect:/visa/my-applications";
    }
}
