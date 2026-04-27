package org.example.visacasemanagementsystem.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.example.visacasemanagementsystem.user.service.UserService;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;

import java.io.IOException;
import java.util.Objects;

@Configuration
public class OauthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;

    public OauthSuccessHandler(UserService userService,
                               SecurityContextRepository securityContextRepository) {
        this.userService = userService;
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        DefaultOidcUser oidcUser = (DefaultOidcUser) Objects.requireNonNull(authentication.getPrincipal());
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        Boolean emailVerified = oidcUser.getAttribute("email_verified");
        if (email == null || !Boolean.TRUE.equals(emailVerified)) {
            throw new BadCredentialsException("OAuth account missing verified email");
        }

        // Persistence + audit run inside a @Transactional service method so the
        // transaction commits before we touch the SecurityContext or issue the redirect.
        User user = userService.findOrCreateOauthUser(email, name);

        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        clearAuthenticationAttributes(request);

        String targetUrl = "/home";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
