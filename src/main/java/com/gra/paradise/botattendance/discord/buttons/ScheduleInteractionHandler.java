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
                .withEmbeds(embedFactory.createAircraftSelectionEmbed())
                .withComponents(ActionRow.of(aircraftSelect));
    }

    public Mono<Void> handleAircraftSelection(SelectMenuInteractionEvent event) {
        String aircraftTypeStr = event.getValues().get(0);
        AircraftType aircraftType = AircraftType.valueOf(aircraftTypeStr);

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
                .withEmbeds(embedFactory.createMissionSelectionEmbed(aircraftType))
                .withComponents(ActionRow.of(missionSelect));
    }

    public Mono<Void> handleMissionSelection(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();
        String[] parts = customId.split(":");
        String aircraftTypeStr = parts[1];
        String missionTypeStr = event.getValues().get(0);

        AircraftType aircraftType = AircraftType.valueOf(aircraftTypeStr);
        MissionType missionType = MissionType.valueOf(missionTypeStr);

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
                    .withEmbeds(embedFactory.createActionSubTypeSelectionEmbed(aircraftType))
                    .withComponents(ActionRow.of(subTypeSelect));
        } else {
            String title = scheduleService.generateGraTitle();
            Button confirmButton = Button.success(
                    "confirm_schedule:" + aircraftTypeStr + ":" + missionTypeStr + ":" + title,
                    "Confirmar"
            );
            Button cancelButton = Button.danger("cancel_schedule", "Cancelar");

            return event.edit()
                    .withEmbeds(embedFactory.createScheduleConfirmationEmbed(aircraftType, missionType, title, null, null))
                    .withComponents(ActionRow.of(confirmButton, cancelButton));
        }
    }

    public Mono<Void> handleActionSubTypeSelection(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();
        String[] parts = customId.split(":");
        String aircraftTypeStr = parts[1];
        String subTypeStr = event.getValues().get(0);

        AircraftType aircraftType = AircraftType.valueOf(aircraftTypeStr);
        ActionSubType subType = ActionSubType.valueOf(subTypeStr);

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
                .withEmbeds(embedFactory.createActionOptionSelectionEmbed(aircraftType, subType))
                .withComponents(ActionRow.of(actionOptionSelect));
    }

    public Mono<Void> handleActionOptionSelection(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();
        String[] parts = customId.split(":");
        String aircraftTypeStr = parts[1];
        String subTypeStr = parts[2];
        String actionOption = event.getValues().get(0);

        AircraftType aircraftType = AircraftType.valueOf(aircraftTypeStr);
        ActionSubType subType = ActionSubType.valueOf(subTypeStr);
        String title = scheduleService.generateGraTitle();

        Button confirmButton = Button.success(
                "confirm_schedule:" + aircraftTypeStr + ":ACTION:" + title + ":" + subTypeStr + ":" + actionOption,
                "Confirmar"
        );
        Button cancelButton = Button.danger("cancel_schedule", "Cancelar");

        return event.edit()
                .withEmbeds(embedFactory.createScheduleConfirmationEmbed(aircraftType, MissionType.ACTION, title, subType, actionOption))
                .withComponents(ActionRow.of(confirmButton, cancelButton));
    }

    public Mono<Void> handleConfirmSchedule(ButtonInteractionEvent event) {
        String[] parts = event.getCustomId().split(":");
        String aircraftTypeStr = parts[1];
        String missionTypeStr = parts[2];
        String title = parts[3];
        ActionSubType actionSubType = parts.length > 4 ? ActionSubType.valueOf(parts[4]) : null;
        String actionOption = parts.length > 5 ? parts[5] : null;

        AircraftType aircraftType = AircraftType.valueOf(aircraftTypeStr);
        MissionType missionType = MissionType.valueOf(missionTypeStr);
        String userId = event.getInteraction().getUser().getId().asString();
        String nickname = event.getInteraction().getMember()
                .map(member -> member.getNickname().orElse(event.getInteraction().getUser().getUsername()))
                .orElse(event.getInteraction().getUser().getUsername());

        return event.deferEdit()
                .then(event.deleteReply())
                .then(Mono.fromCallable(() -> scheduleService.createSchedule(title, aircraftType, missionType, userId, nickname, actionSubType, actionOption)))
                .flatMap(schedule -> messagePublisher.createSchedulePublicMessage(event, schedule))
                .then(scheduleMessageManager.updateSystemMessage());
    }

    public Mono<Void> handleCancelSchedule(ButtonInteractionEvent event) {
        return event.deferEdit()
                .then(event.deleteReply());
    }
}