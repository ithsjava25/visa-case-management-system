package org.example.visacasemanagementsystem.config;

import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
                        .requestMatchers("/user/signup").permitAll()
                        .requestMatchers("/user/login").permitAll()
                        .requestMatchers("/dashboard").authenticated()
                        .requestMatchers("/**/admin").hasRole("ADMIN")
                        .requestMatchers("/**/applicant").hasRole("USER")
                        //TODO: requestMatchers for /**/{userId} endpoints, etc.
                        .anyRequest().hasRole("SYSADMIN")
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .formLogin(l -> l
                        .defaultSuccessUrl("/dashboard", true)
                        .loginPage("/user/login"))
                .logout(withDefaults()) //TODO: Custom logout page required
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

    @Bean
    public CommandLineRunner initData(UserRepository userRepository) {
        return args -> {
            // Skapa en vanlig användare (Applicant)
            if (userRepository.findByEmail("user@test.com").isEmpty()) {
                User user = new User();
                user.setFullName("USER");
                user.setUsername("user@test.com");
                user.setEmail("user@test.com");
                user.setPassword(passwordEncoder().encode("password"));
                user.setUserAuthorization(UserAuthorization.USER);
                userRepository.save(user);
                System.out.println("Testanvändare skapad med ID: " + user.getId());
            }
            // Skapa en admin (Handler)
            if (userRepository.findByEmail("admin@test.com").isEmpty()) {
                User admin = new User();
                admin.setFullName("ADMIN");
                admin.setUsername("admin@test.com");
                admin.setEmail("admin@test.com");
                admin.setPassword(passwordEncoder().encode("password"));
                admin.setUserAuthorization(UserAuthorization.ADMIN);
                userRepository.save(admin);
                System.out.println("Test-admin skapad med ID: " + admin.getId());
            }
            // Skapa en sysadmin
            if (userRepository.findByEmail("sysadmin@test.com").isEmpty()) {
                User sysadmin = new User();
                sysadmin.setFullName("SYSTEM ADMIN");
                sysadmin.setUsername("sysadmin@test.com");
                sysadmin.setEmail("sysadmin@test.com");
                sysadmin.setPassword(passwordEncoder().encode("password"));
                sysadmin.setUserAuthorization(UserAuthorization.SYSADMIN);
                userRepository.save(sysadmin);
                System.out.println("Test-sysadmin skapad med ID: " + sysadmin.getId());
            }
        };
    }


}
