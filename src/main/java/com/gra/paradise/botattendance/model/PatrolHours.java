package com.gra.paradise.botattendance.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "patrol_hours")
public class PatrolHours {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    private PatrolContest contest;
    
    private String discordId;
    private String username;
    
    private Long scheduleId; // Reference to the Schedule that generated these hours
    
    private LocalDate patrolDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private double hours; // Total hours for this session
    
    // Categorization
    private double afternoonHours = 0.0; // Hours during afternoon period
    private double nightHours = 0.0;     // Hours during night period
    
    @Enumerated(EnumType.STRING)
    private PatrolPeriod primaryPeriod; // The period where most time was spent
    
    private boolean valid = true; // Can be set to false if farming detected
    
    public enum PatrolPeriod {
        AFTERNOON,
        NIGHT,
        MIXED,
        OTHER
    }
}