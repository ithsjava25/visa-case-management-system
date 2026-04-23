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
import static org.springframework.security.config.Customizer.withDefaults;

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
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/user/signup").permitAll()
                        .requestMatchers("/user/login").permitAll()
                        .requestMatchers("/user/logout").authenticated()
                        .requestMatchers("/dashboard").authenticated()
                        .requestMatchers("/profile/**").authenticated()
                        .requestMatchers("/visas/**").authenticated()
                        .requestMatchers("/api/comments/**").authenticated()
                        .requestMatchers("/**/admin").hasRole("ADMIN")
                        // /dashboard/applicant was removed — USERs now land on /visas/dashboard
                        // (covered by the /visas/** authenticated matcher above).
                        .anyRequest().hasRole("SYSADMIN")
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .formLogin(l -> l
                        .defaultSuccessUrl("/dashboard", true)
                        .loginPage("/user/login"))
                .logout(AbstractHttpConfigurer::disable)
                .httpBasic(withDefaults());

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
