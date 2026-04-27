package org.example.visacasemanagementsystem.config;

import org.example.visacasemanagementsystem.audit.CommentEventType;
import org.example.visacasemanagementsystem.audit.FileEventType;
import org.example.visacasemanagementsystem.audit.service.CommentLogService;
import org.example.visacasemanagementsystem.audit.service.FileLogService;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.visa.VisaStatus;
import org.example.visacasemanagementsystem.visa.VisaType;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.example.visacasemanagementsystem.visa.repository.VisaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

        @Bean
        public CommandLineRunner initData(UserRepository userRepository,
                                          VisaRepository visaRepository,
                                          org.example.visacasemanagementsystem.comment.repository.CommentRepository commentRepository,
                                          CommentLogService commentLogService,
                                          FileLogService fileLogService,
                                          PasswordEncoder passwordEncoder) {
            return args -> {

                // 1. Initialize Users (Help method further down)
                User applicant = getOrCreateUser(userRepository, passwordEncoder, "user@test.com", "John Doe", UserAuthorization.USER);
                User admin = getOrCreateUser(userRepository, passwordEncoder, "admin@test.com", "Admin Officer", UserAuthorization.ADMIN);
                User sysadmin = getOrCreateUser(userRepository, passwordEncoder, "sysadmin@test.com", "System Admin", UserAuthorization.SYSADMIN);

                // 2. Initialize Visa Cases (Only if database is empty)
                if (visaRepository.count() == 0) {
                    System.out.println("--- Starting generation of test data ---");

                    // CASE A: (INCOMPLETE)
                    Visa incompleteVisa = createVisa(visaRepository, applicant, admin, VisaStatus.INCOMPLETE, VisaType.STUDY, "SWE555666");
                    Long v1Id = incompleteVisa.getId();

                    fileLogService.createFileLog(applicant.getId(), v1Id, "passport_scan.pdf", FileEventType.UPLOADED, "Initial passport scan submitted.");

                    saveCommentAndLog(commentRepository, commentLogService, admin, incompleteVisa, "Page 2 of the passport is missing. Please complete the upload.");
                    saveCommentAndLog(commentRepository, commentLogService, applicant, incompleteVisa, "I am sorry, uploading it now!");

                    fileLogService.createFileLog(applicant.getId(), v1Id, "passport_page2.pdf", FileEventType.UPLOADED, "Supplementary passport page submitted.");

                    // CASE B: (REJECTED)
                    Visa rejectedVisa = createVisa(visaRepository, applicant, admin, VisaStatus.REJECTED, VisaType.EMPLOYMENT, "SWE-REJ-99");
                    saveCommentAndLog(commentRepository, commentLogService, admin, rejectedVisa, "Rejection: Applicant does not meet the income requirements for this category.");

                    // CASE C: (GRANTED)
                    Visa grantedVisa = createVisa(visaRepository, applicant, admin, VisaStatus.GRANTED, VisaType.TOURIST, "SWE-OK-777");
                    saveCommentAndLog(commentRepository, commentLogService, admin, grantedVisa, "All documentation verified. Visa has been granted.");

                    // CASE D: (SUBMITTED)
                    createVisa(visaRepository, applicant, null, VisaStatus.SUBMITTED, VisaType.TOURIST, "NEW-12345");

                    System.out.println("--- Test data generated successfully! ---");
                }
            };
        }

    private void saveCommentAndLog(org.example.visacasemanagementsystem.comment.repository.CommentRepository commentRepo,
                                   CommentLogService logService,
                                   User author, Visa visa, String text) {

        org.example.visacasemanagementsystem.comment.entity.Comment c = new org.example.visacasemanagementsystem.comment.entity.Comment();
        c.setVisa(visa);
        c.setAuthor(author);
        c.setText(text);
        c.setCreatedAt(java.time.LocalDateTime.now());
        c = commentRepo.save(c);

        logService.createCommentLog(author.getId(), visa.getId(), c.getId(), CommentEventType.ADDED, "System generated test comment");
    }

    private User getOrCreateUser(UserRepository repo, PasswordEncoder encoder, String email, String name, UserAuthorization auth) {
        return repo.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setFullName(name);
            u.setUsername(email);
            u.setEmail(email);
            u.setPassword(encoder.encode("password"));
            u.setUserAuthorization(auth);
            return repo.save(u);
        });
    }

    private Visa createVisa(VisaRepository repo, User applicant, User handler, VisaStatus status, VisaType type, String passport) {
        Visa v = new Visa();
        v.setApplicant(applicant);
        v.setHandler(handler);
        v.setVisaStatus(status);
        v.setVisaType(type);
        v.setPassportNumber(passport);
        v.setNationality("Sweden");
        v.setTravelDate(LocalDate.now().plusMonths(2));
        return repo.save(v);
    }
}
