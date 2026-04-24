package org.example.visacasemanagementsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Public landing + static assets + anonymous auth forms
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/css/**").permitAll()
                        .requestMatchers("/user/signup").permitAll()
                        .requestMatchers("/user/login").permitAll()

                        // Any authenticated user can hit these; role-specific protection
                        // lives on the individual @PreAuthorize annotations.
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/user/logout").authenticated()
                        .requestMatchers("/home").authenticated()
                        .requestMatchers("/profile/**").authenticated()
                        .requestMatchers("/visa/**").authenticated()
                        .requestMatchers("/api/comments/**").authenticated()

                        // Audit logs and user list are sysadmin-only.
                        .requestMatchers("/log/**").hasRole("SYSADMIN")
                        .anyRequest().hasRole("SYSADMIN")
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .formLogin(l -> l
                        // /home is the single post-login router and redirects as follows:
                        // USER -> /visa/my-applications
                        // ADMIN -> /visa/cases,
                        // SYSADMIN -> /log/visa
                        .defaultSuccessUrl("/home", true)
                        .loginPage("/user/login"))
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authnProvider = new DaoAuthenticationProvider(userDetailsService);
        authnProvider.setPasswordEncoder(passwordEncoder());
        return authnProvider;
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
