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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import java.io.IOException;
import java.util.Objects;

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

        String email = ((DefaultOidcUser) Objects.requireNonNull(authentication.getPrincipal())).getAttribute("email");
        String name = ((DefaultOidcUser) authentication.getPrincipal()).getAttribute("name");

        if (userRepository.findByEmail(email).isEmpty()) {
            User user = new User();
            user.setFullName(name);
            user.setUsername(email);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("password"));
            user.setUserAuthorization(UserAuthorization.USER);
            userRepository.save(user);
        }

        UserPrincipal principal = new UserPrincipal(userRepository.findByEmail(email).orElseThrow());
        SecurityContext context = new SecurityContextImpl();
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, passwordEncoder.encode("password"), principal.getAuthorities());
        auth.toBuilder().authenticated(true).build();
        context.setAuthentication(auth);

        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);

        clearAuthenticationAttributes(request);

        String targetUrl = "/dashboard";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
