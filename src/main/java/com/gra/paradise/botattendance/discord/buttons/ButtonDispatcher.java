package com.gra.paradise.botattendance.discord.buttons;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ButtonDispatcher {

    private final ScheduleInteractionHandler interactionHandler;
    private final ScheduleActionHandler actionHandler;

    public Mono<Void> handleButtonEvent(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        log.debug("Botão clicado: {}", customId);

        if (customId.equals("create_schedule")) {
            return interactionHandler.handleCreateScheduleButton(event);
        } else if (customId.startsWith("confirm_schedule:")) {
            return interactionHandler.handleConfirmSchedule(event);
        } else if (customId.equals("cancel_schedule")) {
            return interactionHandler.handleCancelSchedule(event);
        } else if (customId.startsWith("board_schedule:")) {
            return actionHandler.handleBoardSchedule(event);
        } else if (customId.startsWith("leave_schedule:")) {
            return actionHandler.handleLeaveSchedule(event);
        } else if (customId.startsWith("end_schedule:")) {
            return actionHandler.handleEndSchedule(event);
        }

        log.warn("Botão não reconhecido: {}", customId);
        return event.reply()
                .withContent("Este botão não está implementado.")
                .withEphemeral(true);
    }

    public Mono<Void> handleSelectMenuEvent(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();
        log.debug("Menu selecionado: {}", customId);

        if (customId.equals("aircraft_select")) {
            return interactionHandler.handleAircraftSelection(event);
        } else if (customId.startsWith("mission_select:")) {
            return interactionHandler.handleMissionSelection(event);
        } else if (customId.startsWith("action_subtype_select:")) {
            return interactionHandler.handleActionSubTypeSelection(event);
        } else if (customId.startsWith("action_option_select:")) {
            return interactionHandler.handleActionOptionSelection(event);
        }

        log.warn("Menu não reconhecido: {}", customId);
        return event.reply()
                .withContent("Este menu não está implementado.")
                .withEphemeral(true);
    }
}