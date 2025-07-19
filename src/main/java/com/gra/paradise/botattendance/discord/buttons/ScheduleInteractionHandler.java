package com.gra.paradise.botattendance.discord.buttons;

import com.gra.paradise.botattendance.model.ActionSubType;
import com.gra.paradise.botattendance.model.AircraftType;
import com.gra.paradise.botattendance.model.MissionType;
import com.gra.paradise.botattendance.model.User;
import com.gra.paradise.botattendance.service.EmbedFactory;
import com.gra.paradise.botattendance.service.ScheduleManager;
import com.gra.paradise.botattendance.service.ScheduleMessageManager;
import com.gra.paradise.botattendance.service.ScheduleMessagePublisher;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.InteractionPresentModalSpec;
import discord4j.rest.http.client.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleInteractionHandler {

    private final ScheduleManager scheduleService;
    private final ScheduleMessageManager scheduleMessageManager;
    private final EmbedFactory embedFactory;
    private final ScheduleMessagePublisher messagePublisher;

    // Mapa temporário para armazenar descrições de OUTROS
    private final Map<String, String> outrosDescriptionCache = new ConcurrentHashMap<>();

    private static final List<String> FUGA_OPTIONS = List.of(
            "Fleeca 68",
            "Joalheria",
            "Joalheria Vangelico"
    );

    private static final List<String> TIRO_OPTIONS = List.of(
            "Tiro",
            "Cassino",
            "Joalheria",
            "Joalheria Vangelico",
            "Banco Central",
            "Banco de Paleto",
            "Banco de Roxwood",
            "Fleeca Invader",
            "Fleeca Praia (Heli Drone)"
    );

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public Mono<Void> handleCreateScheduleButton(ButtonInteractionEvent event) {
        List<SelectMenu.Option> aircraftOptions = new ArrayList<>();
        for (AircraftType aircraftType : AircraftType.values()) {
            aircraftOptions.add(SelectMenu.Option.of(aircraftType.getDisplayName(), aircraftType.name()));
        }

        SelectMenu aircraftSelect = SelectMenu.of("aircraft_select", aircraftOptions)
                .withPlaceholder("Selecione a aeronave")
                .withMaxValues(1);

        return event.reply()
                .withEphemeral(true)
                .withEmbeds(embedFactory.createAircraftSelectionEmbed())
                .withComponents(ActionRow.of(aircraftSelect))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao exibir menu de seleção de aeronave para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao iniciar criação da escala. Tente novamente. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
                })
                .onErrorResume(e -> {
                    log.error("Erro inesperado ao processar botão criar escala: {}", e.getMessage(), e);
                    return event.createFollowup("❌ Erro inesperado. Contate o suporte. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
                });
    }

    public Mono<Void> handleAircraftSelection(SelectMenuInteractionEvent event) {
        if (event.getValues().isEmpty()) {
            log.error("Nenhuma aeronave selecionada pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Selecione uma aeronave válida antes de prosseguir.").withEphemeral(true).then();
        }

        String aircraftTypeStr = event.getValues().get(0);
        AircraftType aircraftType;
        try {
            aircraftType = AircraftType.valueOf(aircraftTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de aeronave inválido '{}' para usuário {}: {}", aircraftTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de aeronave inválido. Escolha uma opção válida.").withEphemeral(true).then();
        }

        List<SelectMenu.Option> missionOptions = new ArrayList<>();
        for (MissionType missionType : MissionType.values()) {
            missionOptions.add(SelectMenu.Option.of(missionType.getDisplayName(), missionType.name()));
        }

        SelectMenu missionSelect = SelectMenu.of("mission_select:" + aircraftTypeStr, missionOptions)
                .withPlaceholder("Selecione o tipo de missão")
                .withMaxValues(1);

        return event.edit()
                .withEmbeds(embedFactory.createMissionSelectionEmbed(aircraftType))
                .withComponents(ActionRow.of(missionSelect))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao exibir menu de seleção de missão para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao selecionar aeronave. Tente novamente.").withEphemeral(true).then();
                })
                .onErrorResume(e -> {
                    log.error("Erro inesperado ao processar seleção de aeronave: {}", e.getMessage(), e);
                    return event.createFollowup("❌ Erro inesperado. Contate o suporte.").withEphemeral(true).then();
                });
    }

    public Mono<Void> handleMissionSelection(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("mission_select:")) {
            log.error("CustomId inválido '{}' para seleção de missão por usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: ID de missão inválido. Reinicie o processo.").withEphemeral(true).then();
        }

        String[] parts = customId.split(":");
        if (parts.length < 2) {
            log.error("Formato de customId inválido '{}' para usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: Formato inválido. Reinicie o processo.").withEphemeral(true).then();
        }

        String aircraftTypeStr = parts[1];
        AircraftType aircraftType;
        try {
            aircraftType = AircraftType.valueOf(aircraftTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de aeronave inválido '{}' para usuário {}: {}", aircraftTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
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
            log.error("Tipo de missão inválido '{}' para usuário {}: {}", missionTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de missão inválido. Escolha uma opção válida.").withEphemeral(true).then();
        }

        String guildId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("Comando deve ser executado em um servidor"));
        String title = scheduleService.generateNextGraTitle(guildId);

        if (missionType == MissionType.ACTION) {
            List<SelectMenu.Option> subTypeOptions = new ArrayList<>();
            for (ActionSubType subType : ActionSubType.values()) {
                subTypeOptions.add(SelectMenu.Option.of(subType.getDisplayName(), subType.name()));
            }

            SelectMenu subTypeSelect = SelectMenu.of("action_subtype_select:" + aircraftTypeStr, subTypeOptions)
                    .withPlaceholder("Selecione o subtipo de ação")
                    .withMaxValues(1);

            return event.edit()
                    .withEmbeds(embedFactory.createActionSubTypeSelectionEmbed(aircraftType))
                    .withComponents(ActionRow.of(subTypeSelect))
                    .onErrorResume(ClientException.class, e -> {
                        log.error("Erro ao exibir menu de subtipo de ação para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                        return event.createFollowup("❌ Erro ao selecionar missão. Tente novamente.").withEphemeral(true).then();
                    })
                    .onErrorResume(e -> {
                        log.error("Erro inesperado ao processar seleção de missão: {}", e.getMessage(), e);
                        return event.createFollowup("❌ Erro inesperado. Contate o suporte.").withEphemeral(true).then();
                    });
        } else if (missionType == MissionType.OUTROS) {
            String modalId = "outros_description_modal:" + aircraftTypeStr + ":" + title + ":" + UUID.randomUUID().toString();
            log.info("Modal criado com customId: {}", modalId);
            TextInput descriptionInput = TextInput.small("outros_description", "Descrição da missão", 1, 100)
                    .placeholder("Digite a descrição da missão (máx. 100 caracteres)");
            InteractionPresentModalSpec modal = InteractionPresentModalSpec.builder()
                    .customId(modalId)
                    .title("Descrição da Missão Outros")
                    .addComponent(ActionRow.of(descriptionInput))
                    .build();

            return event.presentModal(modal)
                    .onErrorResume(ClientException.class, e -> {
                        log.error("Erro ao exibir modal de descrição para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                        return event.createFollowup("❌ Erro ao abrir modal de descrição. Tente novamente. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
                    })
                    .onErrorResume(e -> {
                        log.error("Erro inesperado ao processar seleção de missão OUTROS: {}", e.getMessage(), e);
                        return event.createFollowup("❌ Erro inesperado. Contate o suporte. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
                    });
        } else {
            Button confirmButton = Button.success("confirm_schedule:" + aircraftTypeStr + ":" + missionTypeStr + ":" + title, "Confirmar");
            Button cancelButton = Button.danger("cancel_schedule", "Cancelar");

            return event.edit()
                    .withEmbeds(embedFactory.createScheduleConfirmationEmbed(aircraftType, missionType, title, null, null))
                    .withComponents(ActionRow.of(confirmButton, cancelButton))
                    .onErrorResume(ClientException.class, e -> {
                        log.error("Erro ao exibir confirmação de escala para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                        return event.createFollowup("❌ Erro ao confirmar escala. Tente novamente. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
                    })
                    .onErrorResume(e -> {
                        log.error("Erro inesperado ao processar confirmação de escala: {}", e.getMessage(), e);
                        return event.createFollowup("❌ Erro inesperado. Contate o suporte. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
                    });
        }
    }

    public Mono<Void> handleOutrosDescription(ModalSubmitInteractionEvent event) {
        log.info("Tentativa de processar submissão de modal por usuário {} com customId: {}",
                event.getInteraction().getUser().getId().asString(), event.getCustomId());
        String customId = event.getCustomId();
        if (!customId.startsWith("outros_description_modal:")) {
            log.error("CustomId inválido '{}' para modal de descrição por usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.reply("❌ Erro: ID de modal inválido. Reinicie o processo. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        String[] parts = customId.split(":");
        if (parts.length < 4) {
            log.error("Formato de customId inválido '{}' para usuário {}. Partes: {}", customId, event.getInteraction().getUser().getId().asString(), parts.length);
            return event.reply("❌ Erro: Formato inválido. Reinicie o processo. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        String aircraftTypeStr = parts[1];
        String title = parts[2];
        String sessionId = parts[3];

        AircraftType aircraftType;
        try {
            aircraftType = AircraftType.valueOf(aircraftTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de aeronave inválido '{}' para usuário {}: {}", aircraftTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.reply("❌ Tipo de aeronave inválido. Reinicie o processo. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        Optional<String> descriptionOpt = event.getComponents(TextInput.class)
                .stream()
                .filter(t -> t.getCustomId().equals("outros_description"))
                .findFirst()
                .flatMap(TextInput::getValue);

        if (!descriptionOpt.isPresent() || descriptionOpt.get().trim().isEmpty()) {
            log.error("Descrição não fornecida ou vazia pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.reply("❌ Forneça uma descrição válida para a missão. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        String description = descriptionOpt.get().trim();
        if (description.length() > 100) {
            log.error("Descrição excede o limite de 100 caracteres para usuário {}. Comprimento: {}", event.getInteraction().getUser().getId().asString(), description.length());
            return event.reply("❌ A descrição deve ter no máximo 100 caracteres. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        log.info("Descrição fornecida pelo usuário {}: '{}'", event.getInteraction().getUser().getId().asString(), description);

        // Armazenar a descrição no cache com o sessionId
        outrosDescriptionCache.put(sessionId, description);

        Button confirmButton = Button.success("confirm_schedule:" + aircraftTypeStr + ":OUTROS:" + title + ":" + sessionId, "Confirmar");
        Button cancelButton = Button.danger("cancel_schedule", "Cancelar");

        return event.reply()
                .withEphemeral(true)
                .withEmbeds(embedFactory.createScheduleConfirmationEmbed(aircraftType, MissionType.OUTROS, title, null, description))
                .withComponents(ActionRow.of(confirmButton, cancelButton))
                .doOnSuccess(success -> log.info("Embed de confirmação enviado com sucesso para usuário {}", event.getInteraction().getUser().getId().asString()))
                .doOnError(e -> log.error("Falha ao enviar embed de confirmação para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao processar envio do modal para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.reply("❌ Erro ao processar o envio do formulário. Tente novamente. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
                })
                .onErrorResume(e -> {
                    log.error("Erro inesperado ao processar envio do modal para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.reply("❌ Erro inesperado ao processar o formulário. Contate o suporte. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
                });
    }

    public Mono<Void> handleActionSubTypeSelection(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("action_subtype_select:")) {
            log.error("CustomId inválido '{}' para seleção de subtipo por usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: ID de subtipo inválido. Reinicie o processo.").withEphemeral(true).then();
        }

        String[] parts = customId.split(":");
        if (parts.length < 2) {
            log.error("Formato de customId inválido '{}' para usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: Formato inválido. Reinicie o processo.").withEphemeral(true).then();
        }

        String aircraftTypeStr = parts[1];
        AircraftType aircraftType;
        try {
            aircraftType = AircraftType.valueOf(aircraftTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de aeronave inválido '{}' para usuário {}: {}", aircraftTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de aeronave inválido. Reinicie o processo.").withEphemeral(true).then();
        }

        if (event.getValues().isEmpty()) {
            log.error("Nenhum subtipo selecionado pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Selecione um subtipo válido antes de prosseguir.").withEphemeral(true).then();
        }

        String subTypeStr = event.getValues().get(0);
        ActionSubType subType;
        try {
            subType = ActionSubType.valueOf(subTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Subtipo de ação inválido '{}' para usuário {}: {}", subTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Subtipo de ação inválido. Escolha uma opção válida.").withEphemeral(true).then();
        }

        List<String> options = subType == ActionSubType.FUGA ? FUGA_OPTIONS : TIRO_OPTIONS;
        List<SelectMenu.Option> actionOptions = new ArrayList<>();
        for (String option : options) {
            actionOptions.add(SelectMenu.Option.of(option, option));
        }

        SelectMenu actionOptionSelect = SelectMenu.of("action_option_select:" + aircraftTypeStr + ":" + subTypeStr, actionOptions)
                .withPlaceholder("Selecione a opção de " + subType.getDisplayName().toLowerCase())
                .withMaxValues(1);

        return event.edit()
                .withEmbeds(embedFactory.createActionOptionSelectionEmbed(aircraftType, subType))
                .withComponents(ActionRow.of(actionOptionSelect))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao exibir menu de opção de ação para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao selecionar subtipo. Tente novamente.").withEphemeral(true).then();
                })
                .onErrorResume(e -> {
                    log.error("Erro inesperado ao processar seleção de subtipo: {}", e.getMessage(), e);
                    return event.createFollowup("❌ Erro inesperado. Contate o suporte.").withEphemeral(true).then();
                });
    }

    public Mono<Void> handleActionOptionSelection(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("action_option_select:")) {
            log.error("CustomId inválido '{}' para seleção de opção por usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: ID de opção inválido. Reinicie o processo.").withEphemeral(true).then();
        }

        String[] parts = customId.split(":");
        if (parts.length < 3) {
            log.error("Formato de customId inválido '{}' para usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: Formato inválido. Reinicie o processo.").withEphemeral(true).then();
        }

        String aircraftTypeStr = parts[1];
        String subTypeStr = parts[2];
        if (event.getValues().isEmpty()) {
            log.error("Nenhuma opção selecionada pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Selecione uma opção válida antes de prosseguir.").withEphemeral(true).then();
        }

        String actionOption = event.getValues().get(0);
        AircraftType aircraftType;
        try {
            aircraftType = AircraftType.valueOf(aircraftTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de aeronave inválido '{}' para usuário {}: {}", aircraftTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de aeronave inválido. Reinicie o processo.").withEphemeral(true).then();
        }

        ActionSubType subType;
        try {
            subType = ActionSubType.valueOf(subTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Subtipo de ação inválido '{}' para usuário {}: {}", subTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Subtipo de ação inválido. Reinicie o processo.").withEphemeral(true).then();
        }

        String guildId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("Comando deve ser executado em um servidor"));
        String title = scheduleService.generateNextGraTitle(guildId);

        List<String> validOptions = subType == ActionSubType.FUGA ? FUGA_OPTIONS : TIRO_OPTIONS;
        if (!validOptions.contains(actionOption)) {
            log.error("Opção de ação inválida '{}' para usuário {}: Opção não encontrada em {}", actionOption, event.getInteraction().getUser().getId().asString(), validOptions);
            return event.createFollowup("❌ Opção de ação inválida. Escolha uma opção válida.").withEphemeral(true).then();
        }

        Button confirmButton = Button.success("confirm_schedule:" + aircraftTypeStr + ":ACTION:" + title + ":" + subTypeStr + ":" + actionOption, "Confirmar");
        Button cancelButton = Button.danger("cancel_schedule", "Cancelar");

        return event.deferEdit()
                .then(event.editReply()
                        .withEmbeds(embedFactory.createScheduleConfirmationEmbed(aircraftType, MissionType.ACTION, title, subType, actionOption))
                        .withComponents(ActionRow.of(confirmButton, cancelButton)))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao processar menu action_option_select para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao selecionar opção. Tente novamente.").withEphemeral(true);
                })
                .onErrorResume(e -> {
                    log.error("Erro inesperado ao processar seleção de opção: {}", e.getMessage(), e);
                    return event.createFollowup("❌ Erro inesperado. Contate o suporte.").withEphemeral(true);
                }).then();
    }

    public Mono<Void> handleConfirmSchedule(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("confirm_schedule:")) {
            log.error("CustomId inválido '{}' para confirmação de escala por usuário {}: {}", customId, event.getInteraction().getUser().getId().asString(), customId);
            return event.createFollowup("❌ Erro: ID de confirmação inválido. Reinicie o processo. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        String[] parts = customId.split(":");
        if (parts.length < 4) {
            log.error("Formato de customId inválido '{}' para usuário {}. Partes: {}", customId, event.getInteraction().getUser().getId().asString(), parts.length);
            return event.createFollowup("❌ Erro: Formato inválido. Reinicie o processo. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        String aircraftTypeStr = parts[1];
        String missionTypeStr = parts[2];
        String title = parts[3];
        ActionSubType actionSubType = null;
        String actionOption = null;

        log.debug("Processando confirm_schedule com parts: {}", String.join(", ", parts));

        if (missionTypeStr.equals("ACTION") && parts.length == 6) {
            actionSubType = parseActionSubType(parts[4]);
            actionOption = parts[5];
        } else if (missionTypeStr.equals("OUTROS") && parts.length == 5) {
            String sessionId = parts[4];
            actionOption = outrosDescriptionCache.get(sessionId);
            if (actionOption == null) {
                log.error("Descrição não encontrada para sessionId {} para usuário {}. Cache: {}", sessionId, event.getInteraction().getUser().getId().asString(), outrosDescriptionCache);
                return event.createFollowup("❌ Descrição da missão OUTROS não encontrada. Reinicie o processo.").withEphemeral(true).then();
            }
            outrosDescriptionCache.remove(sessionId); // Limpar o cache após uso
        } else {
            log.error("Formato de customId inesperado para missão {} com {} partes para usuário {}", missionTypeStr, parts.length, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Formato de confirmação inválido. Reinicie o processo. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        AircraftType aircraftType;
        try {
            aircraftType = AircraftType.valueOf(aircraftTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de aeronave inválido '{}' para usuário {}: {}", aircraftTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de aeronave inválido. Reinicie o processo. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        MissionType missionType;
        try {
            missionType = MissionType.valueOf(missionTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de missão inválido '{}' para usuário {}: {}", missionTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de missão inválido. Reinicie o processo. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        if (missionType == MissionType.ACTION && (actionSubType == null || actionOption == null)) {
            log.error("Subtipo ou opção de ação não fornecidos para missão ACTION pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Subtipo e opção de ação são obrigatórios para missões de ação. Reinicie o processo.").withEphemeral(true).then();
        } else if (missionType == MissionType.OUTROS && (actionOption == null || actionOption.trim().isEmpty())) {
            log.error("Descrição não fornecida para missão OUTROS pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Descrição da missão OUTROS é obrigatória. Reinicie o processo.").withEphemeral(true).then();
        } else if (missionType == MissionType.PATROL && actionOption != null) {
            log.error("Opção de ação fornecida para missão PATROL pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Missões de patrulha não devem ter descrição ou subtipo. Reinicie o processo.").withEphemeral(true).then();
        }

        String userId = event.getInteraction().getUser().getId().asString();
        String nickname = event.getInteraction().getMember()
                .map(member -> member.getNickname().orElse(event.getInteraction().getUser().getUsername()))
                .orElse(event.getInteraction().getUser().getUsername());

        String guildId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("Comando deve ser executado em um servidor"));

        ActionSubType finalActionSubType = actionSubType;
        String finalActionOption = actionOption;
        log.info("Tentando criar escala para usuário {} com title: {}, aircraft: {}, mission: {}, actionSubType: {}, actionOption: {}",
                userId, title, aircraftType, missionType, finalActionSubType, finalActionOption);

        return event.deferEdit()
                .then(event.deleteReply())
                .then(Mono.fromCallable(() -> scheduleService.createSchedule(guildId, title, aircraftType, missionType, userId, nickname, finalActionSubType, finalActionOption)))
                .doOnSuccess(schedule -> log.info("Escala criada com sucesso para scheduleId: {}", schedule.getId()))
                .doOnError(e -> log.error("Erro ao criar escala: {}", e.getMessage(), e))
                .flatMap(schedule -> messagePublisher.createSchedulePublicMessage(event, schedule)
                        .thenReturn(schedule))
                .flatMap(schedule -> scheduleMessageManager.updateSystemMessage(guildId))
                .then(event.createFollowup("✅ Escala criada com sucesso! (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true))
                .onErrorResume(e -> {
                    log.error("Erro ao confirmar escala para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    String errorMessage = "❌ Erro ao criar escala: ";
                    if (e instanceof IllegalArgumentException) {
                        errorMessage += e.getMessage() + ". Reinicie o processo.";
                    } else if (e instanceof RuntimeException) {
                        errorMessage += "Falha ao criar escala devido a erro no banco de dados. Tente novamente ou contate o suporte.";
                    } else {
                        errorMessage += "Erro inesperado. Contate o suporte. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")";
                    }
                    return event.createFollowup(errorMessage).withEphemeral(true);
                }).then();
    }

    public Mono<Void> handleCancelSchedule(ButtonInteractionEvent event) {
        return event.deferEdit()
                .then(event.deleteReply())
                .then(event.createFollowup("❌ Criação de escala cancelada. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao cancelar escala para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao cancelar escala. Tente novamente. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true);
                })
                .onErrorResume(e -> {
                    log.error("Erro inesperado ao cancelar escala: {}", e.getMessage(), e);
                    return event.createFollowup("❌ Erro inesperado. Contate o suporte. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true);
                }).then();
    }

    public Mono<Void> handleBoardSchedule(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("board_schedule:")) {
            log.error("CustomId inválido '{}' para embarcar em escala por usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: ID de botão inválido. Verifique a escala. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        String scheduleIdStr = customId.split(":")[1];
        Long scheduleId;
        try {
            scheduleId = Long.parseLong(scheduleIdStr);
        } catch (NumberFormatException e) {
            log.error("ID de escala inválido '{}' para usuário {}", scheduleIdStr, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ ID de escala inválido. Tente novamente. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        String userId = event.getInteraction().getUser().getId().asString();
        String username = event.getInteraction().getUser().getUsername();
        String nickname = event.getInteraction().getMember()
                .map(member -> member.getNickname().orElse(username))
                .orElse(username);

        String guildId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("Comando deve ser executado em um servidor"));

        return Mono.fromCallable(() -> scheduleService.addCrewMember(guildId, scheduleId, userId, username, nickname))
                .flatMap(schedule -> scheduleMessageManager.updateScheduleMessage(String.valueOf(schedule.getId()), schedule.getCrewMembers().stream().map(User::getNickname).toList()))
                .then(event.createFollowup("✅ Você embarcou na escala com sucesso! (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true))
                .onErrorResume(e -> {
                    log.error("Erro ao processar botão board_schedule para escala {}: {}", scheduleId, e.getMessage(), e);
                    String errorMessage = "❌ ";
                    if (e instanceof IllegalArgumentException) {
                        errorMessage += e.getMessage() + ". Verifique a escala.";
                    } else if (e instanceof IllegalStateException) {
                        errorMessage += e.getMessage() + ". Tente novamente.";
                    } else if (e instanceof RuntimeException) {
                        errorMessage += "Falha ao embarcar devido a erro no banco de dados. Tente novamente ou contate o suporte.";
                    } else {
                        errorMessage += "Erro inesperado ao embarcar. Contate o suporte. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")";
                    }
                    return event.createFollowup(errorMessage).withEphemeral(true);
                }).then();
    }

    public Mono<Void> handleLeaveSchedule(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("leave_schedule:")) {
            log.error("CustomId inválido '{}' para deixar escala por usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: ID de botão inválido. Verifique a escala. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        String scheduleIdStr = customId.split(":")[1];
        Long scheduleId;
        try {
            scheduleId = Long.parseLong(scheduleIdStr);
        } catch (NumberFormatException e) {
            log.error("ID de escala inválido '{}' para usuário {}", scheduleIdStr, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ ID de escala inválido. Tente novamente. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        String userId = event.getInteraction().getUser().getId().asString();
        String nickname = event.getInteraction().getMember()
                .map(member -> member.getNickname().orElse(event.getInteraction().getUser().getUsername()))
                .orElse(event.getInteraction().getUser().getUsername());

        String guildId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("Comando deve ser executado em um servidor"));

        return Mono.fromCallable(() -> scheduleService.removeCrewMember(guildId, scheduleId, userId, nickname))
                .flatMap(schedule -> scheduleMessageManager.updateScheduleMessage(String.valueOf(schedule.getId()), schedule.getCrewMembers().stream().map(User::getNickname).toList()))
                .then(event.createFollowup("✅ Você desembarcou da escala com sucesso! (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true))
                .onErrorResume(e -> {
                    log.error("Erro ao processar botão leave_schedule para escala {}: {}", scheduleId, e.getMessage(), e);
                    String errorMessage = "❌ ";
                    if (e instanceof IllegalArgumentException) {
                        errorMessage += e.getMessage() + ". Verifique a escala.";
                    } else if (e instanceof IllegalStateException) {
                        errorMessage += e.getMessage() + ". Tente novamente.";
                    } else if (e instanceof RuntimeException) {
                        errorMessage += "Falha ao desembarcar devido a erro no banco de dados. Tente novamente ou contate o suporte.";
                    } else {
                        errorMessage += "Erro inesperado ao desembarcar. Contate o suporte. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")";
                    }
                    return event.createFollowup(errorMessage).withEphemeral(true);
                }).then();
    }

    public Mono<Void> handleEndSchedule(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("end_schedule:")) {
            log.error("CustomId inválido '{}' para encerrar escala por usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: ID de botão inválido. Verifique a escala. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        String scheduleIdStr = customId.split(":")[1];
        Long scheduleId;
        try {
            scheduleId = Long.parseLong(scheduleIdStr);
        } catch (NumberFormatException e) {
            log.error("ID de escala inválido '{}' para usuário {}", scheduleIdStr, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ ID de escala inválido. Tente novamente. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true).then();
        }

        String userId = event.getInteraction().getUser().getId().asString();
        String nickname = event.getInteraction().getMember()
                .map(member -> member.getNickname().orElse(event.getInteraction().getUser().getUsername()))
                .orElse(event.getInteraction().getUser().getUsername());

        String guildId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("Comando deve ser executado em um servidor"));

        return Mono.fromCallable(() -> scheduleService.closeSchedule(guildId, scheduleId, userId, nickname))
                .flatMap(schedule -> scheduleMessageManager.removeScheduleMessage(String.valueOf(schedule.getId()), guildId)
                        .onErrorResume(e -> {
                            log.warn("Falha ao remover mensagem da escala {}: {}. Tentando prosseguir.", schedule.getId(), e.getMessage());
                            return Mono.empty();
                        }))
                .then(scheduleMessageManager.updateSystemMessage(guildId))
                .then(event.createFollowup("✅ Escala encerrada com sucesso! (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")").withEphemeral(true))
                .onErrorResume(e -> {
                    log.error("Erro ao processar botão end_schedule para escala {}: {}", scheduleId, e.getMessage(), e);
                    String errorMessage = "❌ ";
                    if (e instanceof IllegalArgumentException) {
                        errorMessage += e.getMessage() + ". Verifique a escala.";
                    } else if (e instanceof IllegalStateException) {
                        errorMessage += e.getMessage() + ". Tente novamente.";
                    } else if (e instanceof RuntimeException) {
                        errorMessage += "Falha ao encerrar escala devido a erro no banco de dados. Tente novamente ou contate o suporte.";
                    } else {
                        errorMessage += "Erro inesperado ao encerrar escala. Contate o suporte. (Hora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ")";
                    }
                    return event.createFollowup(errorMessage).withEphemeral(true);
                }).then();
    }

    private ActionSubType parseActionSubType(String subTypeStr) {
        try {
            return ActionSubType.valueOf(subTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Subtipo de ação inválido '{}': {}", subTypeStr, e.getMessage(), e);
            throw new IllegalArgumentException("Subtipo de ação inválido: " + subTypeStr, e);
        }
    }
}