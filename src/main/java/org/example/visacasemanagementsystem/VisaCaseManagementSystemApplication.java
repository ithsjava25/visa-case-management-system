package org.example.visacasemanagementsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class VisaCaseManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(VisaCaseManagementSystemApplication.class, args);
    }

}
