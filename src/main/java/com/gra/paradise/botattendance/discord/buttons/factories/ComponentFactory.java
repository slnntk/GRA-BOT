package com.gra.paradise.botattendance.discord.buttons.factories;

import com.gra.paradise.botattendance.discord.buttons.config.ScheduleInteractionConfig;
import com.gra.paradise.botattendance.model.ActionSubType;
import com.gra.paradise.botattendance.model.AircraftType;
import com.gra.paradise.botattendance.model.MissionType;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.InteractionPresentModalSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Factory for creating Discord UI components.
 * Extracted from ScheduleInteractionHandler to separate UI component creation concerns.
 * Implements Factory Pattern for consistent UI component creation.
 */
@Component
@RequiredArgsConstructor
public class ComponentFactory {

    /**
     * Create aircraft selection menu
     */
    public SelectMenu createAircraftSelectionMenu() {
        List<SelectMenu.Option> aircraftOptions = Arrays.stream(AircraftType.values())
                .map(aircraftType -> SelectMenu.Option.of(aircraftType.getDisplayName(), aircraftType.name()))
                .collect(Collectors.toList());

        return SelectMenu.of("aircraft_select", aircraftOptions)
                .withPlaceholder("Selecione a aeronave")
                .withMaxValues(1);
    }

    /**
     * Create mission selection menu for a given aircraft type
     */
    public SelectMenu createMissionSelectionMenu(String aircraftTypeStr) {
        List<SelectMenu.Option> missionOptions = new ArrayList<>();
        for (MissionType missionType : MissionType.values()) {
            missionOptions.add(SelectMenu.Option.of(missionType.getDisplayName(), missionType.name()));
        }

        return SelectMenu.of("mission_select:" + aircraftTypeStr, missionOptions)
                .withPlaceholder("Selecione o tipo de missão")
                .withMaxValues(1);
    }

    /**
     * Create action sub-type selection menu
     */
    public SelectMenu createActionSubTypeSelectionMenu(String aircraftTypeStr) {
        List<SelectMenu.Option> subTypeOptions = new ArrayList<>();
        for (ActionSubType subType : ActionSubType.values()) {
            subTypeOptions.add(SelectMenu.Option.of(subType.getDisplayName(), subType.name()));
        }

        return SelectMenu.of("action_subtype_select:" + aircraftTypeStr, subTypeOptions)
                .withPlaceholder("Selecione o subtipo de ação")
                .withMaxValues(1);
    }

    /**
     * Create action option selection menu
     */
    public SelectMenu createActionOptionSelectionMenu(String aircraftTypeStr, String subTypeStr, ActionSubType subType) {
        List<String> options = subType == ActionSubType.FUGA ? 
            ScheduleInteractionConfig.FUGA_OPTIONS : 
            ScheduleInteractionConfig.TIRO_OPTIONS;
            
        List<SelectMenu.Option> actionOptions = options.stream()
                .map(option -> SelectMenu.Option.of(option, option))
                .collect(Collectors.toList());

        return SelectMenu.of("action_option_select:" + aircraftTypeStr + ":" + subTypeStr, actionOptions)
                .withPlaceholder("Selecione a opção de " + subType.getDisplayName().toLowerCase())
                .withMaxValues(1);
    }

    /**
     * Create confirmation buttons for schedule creation
     */
    public ActionRow createConfirmationButtons(String aircraftTypeStr, String missionTypeStr, 
                                                   String title, String actionSubTypeStr, String actionOption) {
        String confirmCustomId = "confirm_schedule:" + aircraftTypeStr + ":" + missionTypeStr + ":" + title;
        if (actionSubTypeStr != null) {
            confirmCustomId += ":" + actionSubTypeStr + ":" + actionOption;
        }

        Button confirmButton = Button.primary(confirmCustomId, "✅ Confirmar Escala");
        Button cancelButton = Button.secondary("cancel_schedule", "❌ Cancelar");

        return ActionRow.of(confirmButton, cancelButton);
    }

    /**
     * Create schedule management buttons (board/leave/end)
     */
    public List<ActionRow> createScheduleManagementButtons(Long scheduleId) {
        Button boardButton = Button.primary("board_schedule:" + scheduleId, "🚁 Embarcar");
        Button leaveButton = Button.secondary("leave_schedule:" + scheduleId, "⬇️ Desembarcar");
        Button endButton = Button.danger("end_schedule:" + scheduleId, "🔚 Encerrar Escala");

        return List.of(ActionRow.of(boardButton, leaveButton, endButton));
    }

    /**
     * Create modal for "Outros" description input
     */
    public InteractionPresentModalSpec createOutrosDescriptionModal(String aircraftTypeStr, String title, String sessionId) {
        String modalId = "outros_description_modal:" + aircraftTypeStr + ":" + title + ":" + sessionId;
        TextInput descriptionInput = TextInput.small("outros_description", "Descrição da missão", 1, 100)
                .placeholder("Digite a descrição da missão (máx. 100 caracteres)");
                
        return InteractionPresentModalSpec.builder()
                .customId(modalId)
                .title("Descrição da Missão Outros")
                .addComponent(ActionRow.of(descriptionInput))
                .build();
    }
}