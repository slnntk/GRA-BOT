package com.gra.paradise.botattendance.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class GuildConfig {
    @Id
    private String guildId; // Discord guild ID
    private String systemChannelId; // Channel ID for system messages (schedules)
    private String actionLogChannelId; // Channel ID for ACTION mission logs
    private String patrolLogChannelId; // Channel ID for PATROL mission logs
    private String outrosLogChannelId; // Channel ID for OUTROS mission logs
}