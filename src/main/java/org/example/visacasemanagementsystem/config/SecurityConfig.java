package org.example.visacasemanagementsystem.config;

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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, OauthSuccessHandler oauthSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/static/google-icon.svg").permitAll()
                        .requestMatchers("/user/signup").permitAll()
                        .requestMatchers("/user/login").permitAll()
                        .requestMatchers("/logout").permitAll()
                        .requestMatchers("/dashboard").authenticated()
                        .requestMatchers("/profile/**").authenticated()
                        .requestMatchers("/visas/**").authenticated()
                        .requestMatchers("/api/comments/**").authenticated()
                        .requestMatchers("/**/admin").hasRole("ADMIN")
                        .requestMatchers("/**/applicant").hasRole("USER")
                        .requestMatchers("/profile/edit/{userId}/authorization").hasRole("SYSADMIN")
                        .anyRequest().hasRole("SYSADMIN")
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .formLogin(l -> l
                        .defaultSuccessUrl("/dashboard", true)
                        .loginPage("/user/login"))
                .oauth2Login(l -> l
                        .loginPage("/user/login")
                        .successHandler(oauthSuccessHandler))
                .logout(withDefaults()) //TODO: Custom logout page required
                .httpBasic(withDefaults());

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider authnProvider = new DaoAuthenticationProvider(userDetailsService);
        authnProvider.setPasswordEncoder(passwordEncoder());
        return authnProvider;
    }

    @Bean
    public static PasswordEncoder passwordEncoder(){
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository(){
        return new HttpSessionSecurityContextRepository();
    }
}
