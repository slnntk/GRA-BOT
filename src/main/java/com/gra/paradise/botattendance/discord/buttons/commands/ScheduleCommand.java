package com.gra.paradise.botattendance.discord.buttons.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import reactor.core.publisher.Mono;

/**
 * Interface for schedule interaction commands.
 * Implements Command Pattern for handling different types of Discord interactions.
 */
public interface ScheduleCommand {

    /**
     * Handle button interaction events
     */
    default Mono<Void> handleButton(ButtonInteractionEvent event) {
        return Mono.error(new UnsupportedOperationException("Button handling not implemented"));
    }

    /**
     * Handle select menu interaction events
     */
    default Mono<Void> handleSelectMenu(SelectMenuInteractionEvent event) {
        return Mono.error(new UnsupportedOperationException("Select menu handling not implemented"));
    }

    /**
     * Handle modal submit interaction events
     */
    default Mono<Void> handleModal(ModalSubmitInteractionEvent event) {
        return Mono.error(new UnsupportedOperationException("Modal handling not implemented"));
    }

    /**
     * Check if this command can handle the given custom ID
     */
    boolean canHandle(String customId);
}