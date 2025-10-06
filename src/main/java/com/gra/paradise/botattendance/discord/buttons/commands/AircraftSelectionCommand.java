package com.gra.paradise.botattendance.discord.buttons.commands;

import com.gra.paradise.botattendance.discord.buttons.factories.ComponentFactory;
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
 * Command for handling aircraft selection menu interaction.
 * Implements Command Pattern for the "aircraft_select" menu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AircraftSelectionCommand implements ScheduleCommand {

    private final EmbedFactory embedFactory;
    private final ComponentFactory componentFactory;

    @Override
    public boolean canHandle(String customId) {
        return "aircraft_select".equals(customId);
    }

    @Override
    public Mono<Void> handleSelectMenu(SelectMenuInteractionEvent event) {
        if (event.getValues().isEmpty()) {
            log.error("Nenhuma aeronave selecionada pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Selecione uma aeronave válida antes de prosseguir.").withEphemeral(true).then();
        }

        String aircraftTypeStr = event.getValues().get(0);
        AircraftType aircraftType;
        try {
            aircraftType = AircraftType.valueOf(aircraftTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de aeronave inválido '{}' para usuário {}: {}", aircraftTypeStr, 
                    event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de aeronave inválido. Escolha uma opção válida.").withEphemeral(true).then();
        }

        return event.edit()
                .withEmbeds(embedFactory.createMissionSelectionEmbed(aircraftType))
                .withComponents(ActionRow.of(componentFactory.createMissionSelectionMenu(aircraftTypeStr)))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao exibir menu de seleção de missão para usuário {}: {}", 
                            event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao selecionar aeronave. Tente novamente.").withEphemeral(true).then();
                })
                .onErrorResume(e -> {
                    log.error("Erro inesperado ao processar seleção de aeronave: {}", e.getMessage(), e);
                    return event.createFollowup("❌ Erro inesperado. Contate o suporte.").withEphemeral(true).then();
                });
    }
}