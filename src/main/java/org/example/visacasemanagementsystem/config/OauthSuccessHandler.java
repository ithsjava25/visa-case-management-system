package org.example.visacasemanagementsystem.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class OauthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

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

        if (userRepository.findByEmail(email).isEmpty()) {
            User user = new User();
            user.setFullName(name);
            user.setUsername(email);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setUserAuthorization(UserAuthorization.USER);
            userRepository.save(user);
        }

        UserPrincipal principal = new UserPrincipal(userRepository.findByEmail(email).orElseThrow());
        SecurityContext context = new SecurityContextImpl();
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        context.setAuthentication(auth);

        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);

        clearAuthenticationAttributes(request);

        String targetUrl = "/dashboard";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
