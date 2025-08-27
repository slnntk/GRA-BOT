package com.gra.paradise.botattendance.discord.buttons;

import com.gra.paradise.botattendance.discord.buttons.commands.ScheduleCommand;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Coordinator for schedule interactions that delegates to specific command handlers.
 * Implements Command Pattern by routing interactions to appropriate command implementations.
 * Replaces the monolithic ScheduleInteractionHandler with a modular approach.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleInteractionCoordinator {

    private final List<ScheduleCommand> commands;

    /**
     * Handle button interaction events by delegating to appropriate command
     */
    public Mono<Void> handleButton(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        log.debug("Handling button interaction with customId: {}", customId);

        return findCommand(customId)
                .map(command -> command.handleButton(event))
                .orElseGet(() -> {
                    log.warn("No handler found for button customId: {}", customId);
                    return event.createFollowup("❌ Comando não reconhecido. Contate o suporte.")
                            .withEphemeral(true).then();
                });
    }

    /**
     * Handle select menu interaction events by delegating to appropriate command
     */
    public Mono<Void> handleSelectMenu(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();
        log.debug("Handling select menu interaction with customId: {}", customId);

        return findCommand(customId)
                .map(command -> command.handleSelectMenu(event))
                .orElseGet(() -> {
                    log.warn("No handler found for select menu customId: {}", customId);
                    return event.createFollowup("❌ Menu não reconhecido. Contate o suporte.")
                            .withEphemeral(true).then();
                });
    }

    /**
     * Handle modal submit interaction events by delegating to appropriate command
     */
    public Mono<Void> handleModal(ModalSubmitInteractionEvent event) {
        String customId = event.getCustomId();
        log.debug("Handling modal interaction with customId: {}", customId);

        return findCommand(customId)
                .map(command -> command.handleModal(event))
                .orElseGet(() -> {
                    log.warn("No handler found for modal customId: {}", customId);
                    return event.createFollowup("❌ Modal não reconhecido. Contate o suporte.")
                            .withEphemeral(true).then();
                });
    }

    /**
     * Find the appropriate command for the given customId
     */
    private java.util.Optional<ScheduleCommand> findCommand(String customId) {
        return commands.stream()
                .filter(command -> command.canHandle(customId))
                .findFirst();
    }
}