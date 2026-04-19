package org.example.visacasemanagementsystem.config;

import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer{

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {

            if (userRepository.findByEmail("user@test.com").isEmpty()) {
                User user = new User();
                user.setFullName("USER");
                user.setUsername("user@test.com");
                user.setEmail("user@test.com");
                user.setPassword(passwordEncoder.encode("password"));
                user.setUserAuthorization(UserAuthorization.USER);
                userRepository.save(user);
                System.out.println("Testanvändare skapad med ID: " + user.getId());
            }

            if (userRepository.findByEmail("admin@test.com").isEmpty()) {
                User admin = new User();
                admin.setFullName("ADMIN");
                admin.setUsername("admin@test.com");
                admin.setEmail("admin@test.com");
                admin.setPassword(passwordEncoder.encode("password"));
                admin.setUserAuthorization(UserAuthorization.ADMIN);
                userRepository.save(admin);
                System.out.println("Test-admin skapad med ID: " + admin.getId());
            }

            if (userRepository.findByEmail("sysadmin@test.com").isEmpty()) {
                User sysadmin = new User();
                sysadmin.setFullName("SYSTEM ADMIN");
                sysadmin.setUsername("sysadmin@test.com");
                sysadmin.setEmail("sysadmin@test.com");
                sysadmin.setPassword(passwordEncoder.encode("password"));
                sysadmin.setUserAuthorization(UserAuthorization.SYSADMIN);
                userRepository.save(sysadmin);
                System.out.println("Test-sysadmin skapad med ID: " + sysadmin.getId());
            }
        };
    }

}
