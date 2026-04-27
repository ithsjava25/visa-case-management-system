package org.example.visacasemanagementsystem;

import org.example.visacasemanagementsystem.file.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


@SpringBootTest
@ActiveProfiles("test")
class VisaCaseManagementSystemVisaTests {

    @MockitoBean
    private FileService fileService;

    @Test
    void contextLoads() {
    }

}
