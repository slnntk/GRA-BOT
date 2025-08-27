package com.gra.paradise.botattendance.discord.buttons.commands;

import com.gra.paradise.botattendance.discord.buttons.cache.DescriptionCacheManager;
import com.gra.paradise.botattendance.discord.buttons.config.ScheduleInteractionConfig;
import com.gra.paradise.botattendance.discord.buttons.factories.ComponentFactory;
import com.gra.paradise.botattendance.model.ActionSubType;
import com.gra.paradise.botattendance.model.AircraftType;
import com.gra.paradise.botattendance.model.MissionType;
import com.gra.paradise.botattendance.service.EmbedFactory;
import com.gra.paradise.botattendance.service.ScheduleManager;
import com.gra.paradise.botattendance.service.ScheduleMessageManager;
import com.gra.paradise.botattendance.service.ScheduleMessagePublisher;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.TextInput;
import discord4j.rest.http.client.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Command for handling "Outros" mission description modal submission.
 * Implements Command Pattern for the "outros_description_modal" modal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutrosDescriptionCommand implements ScheduleCommand {

    private final EmbedFactory embedFactory;
    private final ComponentFactory componentFactory;
    private final ScheduleManager scheduleService;
    private final ScheduleMessageManager scheduleMessageManager;
    private final ScheduleMessagePublisher messagePublisher;
    private final DescriptionCacheManager cacheManager;

    @Override
    public boolean canHandle(String customId) {
        return customId.startsWith("outros_description_modal:");
    }

    @Override
    public Mono<Void> handleModal(ModalSubmitInteractionEvent event) {
        String customId = event.getCustomId();
        String[] parts = customId.split(":");
        if (parts.length < 4) {
            log.error("Formato de customId inválido para modal 'outros' '{}' para usuário {}", customId, 
                    event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: Formato inválido. Reinicie o processo.").withEphemeral(true).then();
        }

        String aircraftTypeStr = parts[1];
        String title = parts[2];
        String sessionId = parts[3];

        AircraftType aircraftType;
        try {
            aircraftType = AircraftType.valueOf(aircraftTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de aeronave inválido '{}' para usuário {}: {}", aircraftTypeStr, 
                    event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de aeronave inválido. Reinicie o processo.").withEphemeral(true).then();
        }

        Optional<TextInput> descriptionOpt = event.getComponents(TextInput.class)
                .stream()
                .filter(textInput -> textInput.getCustomId().equals("outros_description"))
                .findFirst();

        String modalId = "outros_description_modal:" + aircraftTypeStr + ":" + title + ":" + sessionId;
        
        if (!descriptionOpt.isPresent() || descriptionOpt.get().getValue().orElse("").trim().isEmpty()) {
            log.error("Descrição não fornecida ou vazia pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.presentModal(componentFactory.createOutrosDescriptionModal(aircraftTypeStr, title, sessionId))
                    .onErrorResume(ClientException.class, e -> {
                        log.error("Erro ao reabrir modal para usuário {}: {}", 
                                event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                        return event.createFollowup("❌ Erro ao reabrir modal. Tente novamente. (Hora: " + 
                                LocalDateTime.now(ZoneId.of("America/Fortaleza"))
                                        .format(ScheduleInteractionConfig.DATE_TIME_FORMATTER) + ")")
                                .withEphemeral(true).then();
                    });
        }

        String description = descriptionOpt.get().getValue().orElse("").trim();
        log.info("Descrição fornecida pelo usuário {}: '{}'", event.getInteraction().getUser().getId().asString(), description);

        // Verificar e limpar cache para evitar colisões
        if (cacheManager.containsKey(sessionId)) {
            log.warn("Cache collision detected for sessionId: {}. Clearing existing entry.", sessionId);
            cacheManager.remove(sessionId);
        }

        cacheManager.put(sessionId, description);

        return event.createFollowup()
                .withEphemeral(true)
                .withEmbeds(embedFactory.createScheduleConfirmationEmbed(aircraftType, MissionType.OUTROS, title, null, null))
                .withComponents(componentFactory.createConfirmationButtons(aircraftTypeStr, "OUTROS", title, null, description))
                .then()
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao processar confirmação para usuário {}: {}", 
                            event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao processar descrição. Tente novamente. (Hora: " + 
                            LocalDateTime.now(ZoneId.of("America/Fortaleza"))
                                    .format(ScheduleInteractionConfig.DATE_TIME_FORMATTER) + ")")
                            .withEphemeral(true).then();
                });
    }
}