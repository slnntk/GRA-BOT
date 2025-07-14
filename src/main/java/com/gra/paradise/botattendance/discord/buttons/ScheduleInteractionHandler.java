package com.gra.paradise.botattendance.discord.buttons;

import com.gra.paradise.botattendance.model.ActionSubType;
import com.gra.paradise.botattendance.model.AircraftType;
import com.gra.paradise.botattendance.model.MissionType;
import com.gra.paradise.botattendance.service.EmbedFactory;
import com.gra.paradise.botattendance.service.ScheduleManager;
import com.gra.paradise.botattendance.service.ScheduleMessageManager;
import com.gra.paradise.botattendance.service.ScheduleMessagePublisher;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.rest.http.client.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleInteractionHandler {

    private final ScheduleManager scheduleService;
    private final ScheduleMessageManager scheduleMessageManager;
    private final EmbedFactory embedFactory;
    private final ScheduleActionHandler actionHandler;
    private final ScheduleMessagePublisher messagePublisher;

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

    public Mono<Void> handleCreateScheduleButton(ButtonInteractionEvent event) {
        List<SelectMenu.Option> aircraftOptions = new ArrayList<>();
        for (AircraftType aircraftType : AircraftType.values()) {
            aircraftOptions.add(SelectMenu.Option.of(
                    aircraftType.getDisplayName(),
                    aircraftType.name()
            ));
        }

        SelectMenu aircraftSelect = SelectMenu.of(
                "aircraft_select",
                aircraftOptions
        ).withPlaceholder("Selecione a aeronave");

        return event.reply()
                .withEphemeral(true)
                .withEmbeds(embedFactory.createAircraftSelectionEmbed().embeds().get()) // Extract embeds
                .withComponents(ActionRow.of(aircraftSelect))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao exibir menu de seleção de aeronave para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao iniciar criação da escala. Tente novamente.").withEphemeral(true).then();
                });
    }

    public Mono<Void> handleAircraftSelection(SelectMenuInteractionEvent event) {
        if (event.getValues().isEmpty()) {
            log.error("Nenhuma aeronave selecionada pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Selecione uma aeronave válida").withEphemeral(true).then();
        }

        String aircraftTypeStr = event.getValues().get(0);
        AircraftType aircraftType;
        try {
            aircraftType = AircraftType.valueOf(aircraftTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de aeronave inválido '{}' para usuário {}: {}", aircraftTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de aeronave inválido").withEphemeral(true).then();
        }

        List<SelectMenu.Option> missionOptions = new ArrayList<>();
        for (MissionType missionType : MissionType.values()) {
            missionOptions.add(SelectMenu.Option.of(
                    missionType.getDisplayName(),
                    missionType.name()
            ));
        }

        SelectMenu missionSelect = SelectMenu.of(
                "mission_select:" + aircraftTypeStr,
                missionOptions
        ).withPlaceholder("Selecione o tipo de missão");

        return event.edit()
                .withEmbeds(embedFactory.createMissionSelectionEmbed(aircraftType).embeds().get()) // Extract embeds
                .withComponents(ActionRow.of(missionSelect))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao exibir menu de seleção de missão para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao selecionar aeronave").withEphemeral(true).then();
                });
    }

    public Mono<Void> handleMissionSelection(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("mission_select:")) {
            log.error("CustomId inválido '{}' para seleção de missão por usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: ID de missão inválido").withEphemeral(true).then();
        }

        String[] parts = customId.split(":");
        if (parts.length < 2) {
            log.error("Formato de customId inválido '{}' para usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: Formato inválido").withEphemeral(true).then();
        }

        String aircraftTypeStr = parts[1];
        AircraftType aircraftType;
        try {
            aircraftType = AircraftType.valueOf(aircraftTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de aeronave inválido '{}' para usuário {}: {}", aircraftTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de aeronave inválido").withEphemeral(true).then();
        }

        if (event.getValues().isEmpty()) {
            log.error("Nenhuma missão selecionada pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Selecione uma missão válida").withEphemeral(true).then();
        }

        String missionTypeStr = event.getValues().get(0);
        MissionType missionType;
        try {
            missionType = MissionType.valueOf(missionTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de missão inválido '{}' para usuário {}: {}", missionTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de missão inválido").withEphemeral(true).then();
        }

        if (missionType == MissionType.ACTION) {
            List<SelectMenu.Option> subTypeOptions = new ArrayList<>();
            for (ActionSubType subType : ActionSubType.values()) {
                subTypeOptions.add(SelectMenu.Option.of(
                        subType.getDisplayName(),
                        subType.name()
                ));
            }

            SelectMenu subTypeSelect = SelectMenu.of(
                    "action_subtype_select:" + aircraftTypeStr,
                    subTypeOptions
            ).withPlaceholder("Selecione o subtipo de ação");

            return event.edit()
                    .withEmbeds(embedFactory.createActionSubTypeSelectionEmbed(aircraftType).embeds().get()) // Extract embeds
                    .withComponents(ActionRow.of(subTypeSelect))
                    .onErrorResume(ClientException.class, e -> {
                        log.error("Erro ao exibir menu de subtipo de ação para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                        return event.createFollowup("❌ Erro ao selecionar missão").withEphemeral(true).then();
                    });
        } else {
            String title = scheduleService.generateGraTitle();
            Button confirmButton = Button.success(
                    "confirm_schedule:" + aircraftTypeStr + ":" + missionTypeStr + ":" + title,
                    "Confirmar"
            );
            Button cancelButton = Button.danger("cancel_schedule", "Cancelar");

            return event.edit()
                    .withEmbeds(embedFactory.createScheduleConfirmationEmbed(aircraftType, missionType, title, null, null).embeds().get()) // Extract embeds
                    .withComponents(ActionRow.of(confirmButton, cancelButton))
                    .onErrorResume(ClientException.class, e -> {
                        log.error("Erro ao exibir confirmação de escala para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                        return event.createFollowup("❌ Erro ao confirmar escala").withEphemeral(true).then();
                    });
        }
    }

    public Mono<Void> handleActionSubTypeSelection(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("action_subtype_select:")) {
            log.error("CustomId inválido '{}' para seleção de subtipo por usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: ID de subtipo inválido").withEphemeral(true).then();
        }

        String[] parts = customId.split(":");
        if (parts.length < 2) {
            log.error("Formato de customId inválido '{}' para usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: Formato inválido").withEphemeral(true).then();
        }

        String aircraftTypeStr = parts[1];
        AircraftType aircraftType;
        try {
            aircraftType = AircraftType.valueOf(aircraftTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de aeronave inválido '{}' para usuário {}: {}", aircraftTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de aeronave inválido").withEphemeral(true).then();
        }

        if (event.getValues().isEmpty()) {
            log.error("Nenhum subtipo selecionado pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Selecione um subtipo válido").withEphemeral(true).then();
        }

        String subTypeStr = event.getValues().get(0);
        ActionSubType subType;
        try {
            subType = ActionSubType.valueOf(subTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Subtipo de ação inválido '{}' para usuário {}: {}", subTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Subtipo de ação inválido").withEphemeral(true).then();
        }

        List<String> options = subType == ActionSubType.FUGA ? FUGA_OPTIONS : TIRO_OPTIONS;
        List<SelectMenu.Option> actionOptions = new ArrayList<>();
        for (String option : options) {
            actionOptions.add(SelectMenu.Option.of(option, option));
        }

        SelectMenu actionOptionSelect = SelectMenu.of(
                "action_option_select:" + aircraftTypeStr + ":" + subTypeStr,
                actionOptions
        ).withPlaceholder("Selecione a opção de " + subType.getDisplayName().toLowerCase());

        return event.edit()
                .withEmbeds(embedFactory.createActionOptionSelectionEmbed(aircraftType, subType).embeds().get()) // Extract embeds
                .withComponents(ActionRow.of(actionOptionSelect))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao exibir menu de opção de ação para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao selecionar subtipo").withEphemeral(true).then();
                });
    }

    public Mono<Void> handleActionOptionSelection(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("action_option_select:")) {
            log.error("CustomId inválido '{}' para seleção de opção por usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: ID de opção inválido").withEphemeral(true).then();
        }

        String[] parts = customId.split(":");
        if (parts.length < 3) {
            log.error("Formato de customId inválido '{}' para usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: Formato inválido").withEphemeral(true).then();
        }

        String aircraftTypeStr = parts[1];
        String subTypeStr = parts[2];
        if (event.getValues().isEmpty()) {
            log.error("Nenhuma opção selecionada pelo usuário {}", event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Selecione uma opção válida").withEphemeral(true).then();
        }

        String actionOption = event.getValues().get(0);
        AircraftType aircraftType;
        try {
            aircraftType = AircraftType.valueOf(aircraftTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de aeronave inválido '{}' para usuário {}: {}", aircraftTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de aeronave inválido").withEphemeral(true).then();
        }

        ActionSubType subType;
        try {
            subType = ActionSubType.valueOf(subTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Subtipo de ação inválido '{}' para usuário {}: {}", subTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Subtipo de ação inválido").withEphemeral(true).then();
        }

        List<String> validOptions = subType == ActionSubType.FUGA ? FUGA_OPTIONS : TIRO_OPTIONS;
        if (!validOptions.contains(actionOption)) {
            log.error("Opção de ação inválida '{}' para usuário {}: Opção não encontrada em {}", actionOption, event.getInteraction().getUser().getId().asString(), validOptions);
            return event.createFollowup("❌ Opção de ação inválida").withEphemeral(true).then();
        }

        String title = scheduleService.generateGraTitle();
        Button confirmButton = Button.success(
                "confirm_schedule:" + aircraftTypeStr + ":ACTION:" + title + ":" + subTypeStr + ":" + actionOption,
                "Confirmar"
        );
        Button cancelButton = Button.danger("cancel_schedule", "Cancelar");

        return event.deferEdit()
                .then(event.editReply()
                        .withEmbeds(embedFactory.createScheduleConfirmationEmbed(aircraftType, MissionType.ACTION, title, subType, actionOption).embeds().get()) // Extract embeds
                        .withComponents(ActionRow.of(confirmButton, cancelButton)))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao processar menu action_option_select para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao selecionar opção: " + e.getMessage())
                            .withEphemeral(true);
                }).then();
    }

    public Mono<Void> handleConfirmSchedule(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("confirm_schedule:")) {
            log.error("CustomId inválido '{}' para confirmação de escala por usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: ID de confirmação inválido").withEphemeral(true).then();
        }

        String[] parts = customId.split(":");
        if (parts.length < 4) {
            log.error("Formato de customId inválido '{}' para usuário {}", customId, event.getInteraction().getUser().getId().asString());
            return event.createFollowup("❌ Erro: Formato inválido").withEphemeral(true).then();
        }

        String aircraftTypeStr = parts[1];
        String missionTypeStr = parts[2];
        String title = parts[3];
        ActionSubType actionSubType = parts.length > 4 ? parseActionSubType(parts[4]) : null;
        String actionOption = parts.length > 5 ? parts[5] : null;

        AircraftType aircraftType;
        try {
            aircraftType = AircraftType.valueOf(aircraftTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de aeronave inválido '{}' para usuário {}: {}", aircraftTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de aeronave inválido").withEphemeral(true).then();
        }

        MissionType missionType;
        try {
            missionType = MissionType.valueOf(missionTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de missão inválido '{}' para usuário {}: {}", missionTypeStr, event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
            return event.createFollowup("❌ Tipo de missão inválido").withEphemeral(true).then();
        }

        if (missionType == MissionType.ACTION && actionSubType != null && actionOption != null) {
            List<String> validOptions = actionSubType == ActionSubType.FUGA ? FUGA_OPTIONS : TIRO_OPTIONS;
            if (!validOptions.contains(actionOption)) {
                log.error("Opção de ação inválida '{}' para usuário {}: Opção não encontrada em {}", actionOption, event.getInteraction().getUser().getId().asString(), validOptions);
                return event.createFollowup("❌ Opção de ação inválida").withEphemeral(true).then();
            }
        }

        String userId = event.getInteraction().getUser().getId().asString();
        String nickname = event.getInteraction().getMember()
                .map(member -> member.getNickname().orElse(event.getInteraction().getUser().getUsername()))
                .orElse(event.getInteraction().getUser().getUsername());

        return event.deferEdit()
                .then(event.deleteReply())
                .then(Mono.fromCallable(() -> {
                    try {
                        return scheduleService.createSchedule(title, aircraftType, missionType, userId, nickname, actionSubType, actionOption);
                    } catch (Exception e) {
                        log.error("Erro ao criar escala para usuário {}: {}", userId, e.getMessage(), e);
                        throw e;
                    }
                }))
                .flatMap(schedule -> messagePublisher.createSchedulePublicMessage(event, schedule))
                .then(scheduleMessageManager.updateSystemMessage())
                .then(event.createFollowup("Escala criada com sucesso!").withEphemeral(true))
                .onErrorResume(e -> {
                    log.error("Erro ao confirmar escala para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    String errorMessage;
                    if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
                        errorMessage = e.getMessage();
                    } else if (e instanceof RuntimeException) {
                        errorMessage = "Erro ao criar escala no banco de dados";
                    } else {
                        errorMessage = "Erro inesperado ao criar escala. Contate o suporte.";
                    }
                    return event.createFollowup("❌ " + errorMessage).withEphemeral(true);
                }).then();
    }

    public Mono<Void> handleCancelSchedule(ButtonInteractionEvent event) {
        return event.deferEdit()
                .then(event.deleteReply())
                .then(event.createFollowup("Criação de escala cancelada").withEphemeral(true))
                .onErrorResume(ClientException.class, e -> {
                    log.error("Erro ao cancelar escala para usuário {}: {}", event.getInteraction().getUser().getId().asString(), e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao cancelar escala").withEphemeral(true);
                }).then();
    }

    private ActionSubType parseActionSubType(String subTypeStr) {
        try {
            return ActionSubType.valueOf(subTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Subtipo de ação inválido '{}': {}", subTypeStr, e.getMessage(), e);
            throw new IllegalArgumentException("Subtipo de ação inválido", e);
        }
    }
}