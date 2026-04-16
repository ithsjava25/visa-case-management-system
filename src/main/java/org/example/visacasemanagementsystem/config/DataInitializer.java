package org.example.visacasemanagementsystem.config;

import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer{

    @Bean
    public CommandLineRunner initData(UserRepository userRepository) {
        return args -> {

            if (userRepository.findByEmail("user@test.com").isEmpty()) {
                User user = new User();
                user.setFullName("USER");
                user.setEmail("user@test.com");
                user.setPassword("password");
                user.setUserAuthorization(UserAuthorization.USER);
                userRepository.save(user);
                System.out.println("Testanvändare skapad med ID: " + user.getId());

                User admin = new User();
                admin.setFullName("ADMIN");
                admin.setEmail("user@test.com2");
                admin.setPassword("password");
                admin.setUserAuthorization(UserAuthorization.ADMIN);
                userRepository.save(admin);
                System.out.println("Test-admin skapad med ID: " + admin.getId());
            }

            if (userRepository.findByEmail("admin@test.com").isEmpty()) {
                User sysadmin = new User();
                sysadmin.setFullName("SYSTEM ADMIN");
                sysadmin.setEmail("admin@test.com");
                sysadmin.setPassword("password");
                sysadmin.setUserAuthorization(UserAuthorization.SYSADMIN);
                userRepository.save(sysadmin);
                System.out.println("Test-sysadmin skapad med ID: " + sysadmin.getId());
            }
        };
    }

}
