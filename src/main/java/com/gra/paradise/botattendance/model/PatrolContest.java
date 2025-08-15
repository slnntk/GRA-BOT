package com.gra.paradise.botattendance.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "patrol_contests")
public class PatrolContest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String guildId;
    private String title;
    private String description;
    
    private Instant startDate;
    private Instant endDate;
    
    // Contest requirements
    private int requiredHours = 18; // Default 18 hours
    private double maxDailyHours = 4.5; // Default 4.5 hours max per day
    
    // Time periods for different rewards
    private String afternoonStart = "13:00"; // 13:00
    private String afternoonEnd = "18:00";   // 18:00
    private String nightStart = "19:00";     // 19:00
    private String nightEnd = "00:00";       // 00:00 (midnight)
    
    private boolean active = true;
    private Instant createdAt;
    private String createdBy;
    
    // Rewards configuration
    private int afternoonWinners = 3; // Number of afternoon winners
    private int nightVipWinners = 2;  // Number of night VIP winners
    
    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatrolParticipant> participants = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}