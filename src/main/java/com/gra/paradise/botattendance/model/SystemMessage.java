package com.gra.paradise.botattendance.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class SystemMessage {
    @Id
    private String guildId; // Guild ID as the primary key
    private String channelId; // Channel ID for the system message
    private String messageId; // Message ID for the system message
}