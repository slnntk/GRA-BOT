package com.gra.paradise.botattendance.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "schedule_logs")
public class ScheduleLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    private LocalDateTime timestamp;
    private String action;
    private String userId;
    private String username;
    private String details;

    protected ScheduleLog() {}

    public ScheduleLog(Schedule schedule, String action, String userId, String username, String details) {
        this.schedule = schedule;
        this.timestamp = LocalDateTime.now();
        this.action = action;
        this.userId = userId;
        this.username = username;
        this.details = details;
    }

    @Override
    public String toString() {
        return "ScheduleLog{" +
                "id=" + id +
                ", scheduleId=" + (schedule != null ? schedule.getId() : "null") +
                ", timestamp=" + (timestamp != null ? timestamp.toString() : "null") +
                ", action='" + action + '\'' +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}