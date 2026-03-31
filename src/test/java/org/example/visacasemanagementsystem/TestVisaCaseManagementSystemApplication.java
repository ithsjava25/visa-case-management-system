package org.example.visacasemanagementsystem;

import org.springframework.boot.SpringApplication;

public class TestVisaCaseManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.from(VisaCaseManagementSystemApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
