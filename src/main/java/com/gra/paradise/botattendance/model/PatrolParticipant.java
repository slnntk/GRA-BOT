package com.gra.paradise.botattendance.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "patrol_participants")
public class PatrolParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    private PatrolContest contest;
    
    private String discordId;
    private String username;
    private String nickname;
    
    // Calculated patrol hours
    private double totalAfternoonHours = 0.0;
    private double totalNightHours = 0.0;
    private double totalHours = 0.0;
    
    private boolean eligible = false;
    private boolean afternoonEligible = false;
    private boolean nightEligible = false;
    
    // Lottery results
    private boolean afternoonWinner = false;
    private boolean nightVipWinner = false;
    
    private Instant lastUpdated;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
        // Calculate eligibility
        eligible = totalHours >= contest.getRequiredHours();
        afternoonEligible = totalAfternoonHours > 0 && eligible;
        nightEligible = totalNightHours > 0 && eligible;
    }
}