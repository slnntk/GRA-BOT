package com.gra.paradise.botattendance.discord.buttons.commands;

import com.gra.paradise.botattendance.discord.buttons.factories.ComponentFactory;
import com.gra.paradise.botattendance.model.ActionSubType;
import com.gra.paradise.botattendance.model.AircraftType;
import com.gra.paradise.botattendance.service.EmbedFactory;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.rest.http.client.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Command for handling action sub-type selection menu interaction.
 * Implements Command Pattern for the "action_subtype_select" menu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActionSubTypeSelectionCommand implements ScheduleCommand {

    private final EmbedFactory embedFactory;
    private final ComponentFactory componentFactory;

    @Override
    public boolean canHandle(String customId) {
        return customId.startsWith("action_subtype_select:");
    }

    @Override
    public Mono<Void> handleSelectMenu(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();
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
            log.error("Nenhum subtipo de ação selecionado pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Selecione um subtipo de ação válido antes de prosseguir.").withEphemeral(true).then();
        }

        String subTypeStr = event.getValues().get(0);
        ActionSubType subType;
        try {
            subType = ActionSubType.valueOf(subTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Subtipo de ação inválido '{}' para usuário {}: {}", subTypeStr, 
                    event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Subtipo de ação inválido. Escolha uma opção válida.").withEphemeral(true).then();
        }

        return event.edit()
                .withEmbeds(embedFactory.createActionOptionSelectionEmbed(aircraftType, subType))
                .withComponents(ActionRow.of(componentFactory.createActionOptionSelectionMenu(aircraftTypeStr, subTypeStr, subType)))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao exibir menu de opções de ação para usuário {}: {}", 
                            event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao selecionar subtipo de ação. Tente novamente.").withEphemeral(true).then();
                })
                .onErrorResume(e -> {
                    log.error("Erro inesperado ao processar seleção de subtipo de ação: {}", e.getMessage(), e);
                    return event.createFollowup("❌ Erro inesperado. Contate o suporte.").withEphemeral(true).then();
                });
    }
}