package org.example.visacasemanagementsystem.log.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.example.visacasemanagementsystem.log.LogEvent;
import java.time.LocalDateTime;

@Entity
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @NotNull
    private LocalDateTime timeStamp;

    @NotNull
    private Long userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private LogEvent logEvent;
}
