package com.gra.paradise.botattendance.discord.buttons;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "discord.enabled", havingValue = "true", matchIfMissing = true)
public class ButtonDispatcher {

    private final ScheduleInteractionHandler interactionHandler;
    private final ScheduleActionHandler actionHandler;
    private final Set<String> acknowledgedInteractions = ConcurrentHashMap.newKeySet();

    public Mono<Void> handleButtonEvent(ButtonInteractionEvent event) {
        String interactionId = event.getInteraction().getId().asString();
        if (acknowledgedInteractions.contains(interactionId)) {
            log.debug("Interaction {} already processed", interactionId);
            return Mono.empty();
        }
        acknowledgedInteractions.add(interactionId);

        String customId = event.getCustomId();
        log.info("Botão clicado por usuário {}: {}", event.getInteraction().getUser().getId().asString(), customId);

        if (customId.equals("create_schedule")) {
            return interactionHandler.handleCreateScheduleButton(event);
        } else if (customId.startsWith("confirm_schedule:")) {
            log.debug("Processando confirmação de escala com customId: {}", customId);
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

        log.warn("Botão não reconhecido: {} para usuário {}", customId, event.getInteraction().getUser().getId().asString());
        return event.reply()
                .withContent("❌ Este botão não está implementado. Contate o suporte.")
                .withEphemeral(true);
    }

    public Mono<Void> handleSelectMenuEvent(SelectMenuInteractionEvent event) {
        String interactionId = event.getInteraction().getId().asString();
        if (acknowledgedInteractions.contains(interactionId)) {
            log.debug("Interaction {} already processed", interactionId);
            return Mono.empty();
        }
        acknowledgedInteractions.add(interactionId);

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
                .withContent("❌ Este menu não está implementado.")
                .withEphemeral(true);
    }

    public Mono<Void> handleModalSubmitEvent(ModalSubmitInteractionEvent event) {
        String interactionId = event.getInteraction().getId().asString();
        if (acknowledgedInteractions.contains(interactionId)) {
            log.debug("Interaction {} already processed", interactionId);
            return Mono.empty();
        }
        acknowledgedInteractions.add(interactionId);

        String customId = event.getCustomId();
        log.debug("Modal enviado: {}", customId);

        if (customId.startsWith("outros_description_modal:")) {
            return interactionHandler.handleOutrosDescription(event)
                    .doOnSuccess(success -> log.info("Submissão de modal processada com sucesso para customId: {}", customId))
                    .doOnError(e -> log.error("Falha ao processar submissão de modal com customId {}: {}", customId, e.getMessage(), e));
        }

        log.warn("Modal não reconhecido: {}", customId);
        return event.reply()
                .withContent("❌ Este modal não está implementado.")
                .withEphemeral(true);
    }
}