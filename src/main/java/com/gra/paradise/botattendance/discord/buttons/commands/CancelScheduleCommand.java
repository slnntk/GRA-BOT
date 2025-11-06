package com.gra.paradise.botattendance.discord.buttons.commands;

import com.gra.paradise.botattendance.discord.buttons.config.ScheduleInteractionConfig;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Command for handling schedule cancellation button interaction.
 * Implements Command Pattern for the "cancel_schedule" button.
 */
@Slf4j
@Component
public class CancelScheduleCommand implements ScheduleCommand {

    @Override
    public boolean canHandle(String customId) {
        return "cancel_schedule".equals(customId);
    }

    @Override
    public Mono<Void> handleButton(ButtonInteractionEvent event) {
        log.info("Usuário {} cancelou a criação de escala", event.getInteraction().getUser().getId().asString());
        
        return event.createFollowup("❌ Criação de escala cancelada. (Hora: " + 
                LocalDateTime.now(ZoneId.of("America/Fortaleza")).format(ScheduleInteractionConfig.DATE_TIME_FORMATTER) + ")")
                .withEphemeral(true).then();
    }
}