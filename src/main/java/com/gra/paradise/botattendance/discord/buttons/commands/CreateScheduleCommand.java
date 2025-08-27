package com.gra.paradise.botattendance.discord.buttons.commands;

import com.gra.paradise.botattendance.discord.buttons.config.ScheduleInteractionConfig;
import com.gra.paradise.botattendance.discord.buttons.factories.ComponentFactory;
import com.gra.paradise.botattendance.service.EmbedFactory;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.rest.http.client.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Command for handling initial schedule creation button interaction.
 * Implements Command Pattern for the "create_schedule" button.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateScheduleCommand implements ScheduleCommand {

    private final EmbedFactory embedFactory;
    private final ComponentFactory componentFactory;

    @Override
    public boolean canHandle(String customId) {
        return "create_schedule".equals(customId);
    }

    @Override
    public Mono<Void> handleButton(ButtonInteractionEvent event) {
        return event.reply()
                .withEphemeral(true)
                .withEmbeds(embedFactory.createAircraftSelectionEmbed())
                .withComponents(ActionRow.of(componentFactory.createAircraftSelectionMenu()))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao exibir menu de seleção de aeronave para usuário {}: {}", 
                            event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao iniciar criação da escala. Tente novamente. (Hora: " + 
                            LocalDateTime.now(ZoneId.of("America/Fortaleza")).format(ScheduleInteractionConfig.DATE_TIME_FORMATTER) + ")")
                            .withEphemeral(true).then();
                })
                .onErrorResume(e -> {
                    log.error("Erro inesperado ao processar botão criar escala: {}", e.getMessage(), e);
                    return event.createFollowup("❌ Erro inesperado. Contate o suporte. (Hora: " + 
                            LocalDateTime.now(ZoneId.of("America/Fortaleza")).format(ScheduleInteractionConfig.DATE_TIME_FORMATTER) + ")")
                            .withEphemeral(true).then();
                });
    }
}