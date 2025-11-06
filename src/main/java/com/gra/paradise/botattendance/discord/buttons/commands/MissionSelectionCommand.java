package com.gra.paradise.botattendance.discord.buttons.commands;

import com.gra.paradise.botattendance.discord.buttons.factories.ComponentFactory;
import com.gra.paradise.botattendance.model.AircraftType;
import com.gra.paradise.botattendance.model.MissionType;
import com.gra.paradise.botattendance.service.EmbedFactory;
import com.gra.paradise.botattendance.service.ScheduleManager;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;
import discord4j.rest.http.client.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * Command for handling mission selection menu interaction.
 * Implements Command Pattern for the "mission_select" menu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionSelectionCommand implements ScheduleCommand {

    private final EmbedFactory embedFactory;
    private final ComponentFactory componentFactory;
    private final ScheduleManager scheduleService;

    @Override
    public boolean canHandle(String customId) {
        return customId.startsWith("mission_select:");
    }

    @Override
    public Mono<Void> handleSelectMenu(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("mission_select:")) {
            log.error("CustomId inválido '{}' para seleção de missão por usuário {}", customId, 
                    event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: ID de missão inválido. Reinicie o processo.").withEphemeral(true).then();
        }

        String[] parts = customId.split(":");
        if (parts.length < 2) {
            log.error("Formato de customId inválido '{}' para usuário {}", customId, 
                    event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: Formato inválido. Reinicie o processo.").withEphemeral(true).then();
        }

        String aircraftTypeStr = parts[1];
        AircraftType aircraftType;
        try {
            aircraftType = AircraftType.valueOf(aircraftTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de aeronave inválido '{}' para usuário {}: {}", aircraftTypeStr, 
                    event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de aeronave inválido. Reinicie o processo.").withEphemeral(true).then();
        }

        if (event.getValues().isEmpty()) {
            log.error("Nenhuma missão selecionada pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Selecione uma missão válida antes de prosseguir.").withEphemeral(true).then();
        }

        String missionTypeStr = event.getValues().get(0);
        MissionType missionType;
        try {
            missionType = MissionType.valueOf(missionTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de missão inválido '{}' para usuário {}: {}", missionTypeStr, 
                    event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de missão inválido. Escolha uma opção válida.").withEphemeral(true).then();
        }

        String guildId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("Comando deve ser executado em um servidor"));
        String title = scheduleService.generateNextGraTitle(guildId);

        if (missionType == MissionType.ACTION) {
            return handleActionMissionType(event, aircraftType, aircraftTypeStr);
        } else if (missionType == MissionType.OUTROS) {
            return handleOutrosMissionType(event, aircraftTypeStr, title);
        } else {
            // PATROL or other mission types - proceed directly to confirmation
            return event.edit()
                    .withEmbeds(embedFactory.createScheduleConfirmationEmbed(aircraftType, missionType, title, null, null))
                    .withComponents(componentFactory.createConfirmationButtons(aircraftTypeStr, missionTypeStr, title, null, null))
                    .onErrorResume(ClientException.class, e -> {
                        log.error("Erro ao exibir confirmação para usuário {}: {}", 
                                event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                        return event.createFollowup("❌ Erro ao processar missão. Tente novamente.").withEphemeral(true).then();
                    });
        }
    }

    private Mono<Void> handleActionMissionType(SelectMenuInteractionEvent event, AircraftType aircraftType, String aircraftTypeStr) {
        return event.edit()
                .withEmbeds(embedFactory.createActionSubTypeSelectionEmbed(aircraftType))
                .withComponents(ActionRow.of(componentFactory.createActionSubTypeSelectionMenu(aircraftTypeStr)))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao exibir menu de subtipo de ação para usuário {}: {}", 
                            event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao selecionar missão. Tente novamente.").withEphemeral(true).then();
                })
                .onErrorResume(e -> {
                    log.error("Erro inesperado ao processar seleção de missão: {}", e.getMessage(), e);
                    return event.createFollowup("❌ Erro inesperado. Contate o suporte.").withEphemeral(true).then();
                });
    }

    private Mono<Void> handleOutrosMissionType(SelectMenuInteractionEvent event, String aircraftTypeStr, String title) {
        String sessionId = UUID.randomUUID().toString();
        String modalId = "outros_description_modal:" + aircraftTypeStr + ":" + title + ":" + sessionId;
        log.info("Modal criado com customId: {}", modalId);

        return event.presentModal(componentFactory.createOutrosDescriptionModal(aircraftTypeStr, title, sessionId))
                .then(Mono.defer(() -> {
                    Optional<Message> optionalMessage = event.getMessage();
                    if (optionalMessage.isPresent()) {
                        return optionalMessage.get().delete().then();
                    } else {
                        return Mono.empty();
                    }
                }))
                .onErrorResume(ClientException.class, e -> {
                    if (e.getStatus().code() == 404) {
                        log.warn("Mensagem já deletada ou não encontrada para usuário {}: {}", 
                                event.getInteraction().getUser().getId().asString(), e.getMessage());
                        return Mono.empty();
                    } else {
                        log.error("Erro ao processar modal para usuário {}: {}", 
                                event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                        return event.createFollowup("❌ Erro ao abrir modal. Tente novamente.").withEphemeral(true).then();
                    }
                })
                .onErrorResume(e -> {
                    log.error("Erro inesperado ao processar missão 'Outros': {}", e.getMessage(), e);
                    return event.createFollowup("❌ Erro inesperado. Contate o suporte.").withEphemeral(true).then();
                });
    }
}