package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.model.AircraftType;
import com.gra.paradise.botattendance.model.MissionType;
import com.gra.paradise.botattendance.model.ActionSubType;
import com.gra.paradise.botattendance.model.Schedule;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EmbedFactory {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    public EmbedCreateSpec createAircraftSelectionEmbed() {
        return EmbedCreateSpec.builder()
                .title("Criar Nova Escala")
                .description("Selecione a aeronave para a escala.")
                .color(Color.BLUE)
                .timestamp(Instant.now())
                .build();
    }

    public EmbedCreateSpec createMissionSelectionEmbed(AircraftType aircraftType) {
        return EmbedCreateSpec.builder()
                .title("Criar Nova Escala")
                .description("Aeronave selecionada: **" + aircraftType.getDisplayName() + "**\nSelecione o tipo de missão.")
                .color(Color.BLUE)
                .timestamp(Instant.now())
                .build();
    }

    public EmbedCreateSpec createActionSubTypeSelectionEmbed(AircraftType aircraftType) {
        return EmbedCreateSpec.builder()
                .title("Criar Nova Escala")
                .description("Aeronave selecionada: **" + aircraftType.getDisplayName() + "**\nSelecione o subtipo de ação.")
                .color(Color.BLUE)
                .timestamp(Instant.now())
                .build();
    }

    public EmbedCreateSpec createActionOptionSelectionEmbed(AircraftType aircraftType, ActionSubType subType) {
        return EmbedCreateSpec.builder()
                .title("Criar Nova Escala")
                .description("Aeronave selecionada: **" + aircraftType.getDisplayName() + "**\nSubtipo de ação: **" + subType.getDisplayName() + "**\nSelecione a opção.")
                .color(Color.BLUE)
                .timestamp(Instant.now())
                .build();
    }

    public EmbedCreateSpec createScheduleConfirmationEmbed(AircraftType aircraftType, MissionType missionType, String title, ActionSubType actionSubType, String actionOption) {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                .title("Confirmação de Escala")
                .description("Confirme os detalhes da escala:")
                .addField("Título", title, true)
                .addField("Aeronave", aircraftType.getDisplayName(), true)
                .addField("Tipo de Missão", missionType.getDisplayName(), true);

        if (actionSubType != null && actionOption != null) {
            builder.addField("Subtipo de Ação", actionSubType.getDisplayName(), true)
                    .addField("Opção", actionOption, true);
        }

        return builder.color(Color.BLUE)
                .timestamp(Instant.now())
                .build();
    }

    public EmbedCreateSpec createSchedulePublicEmbed(Schedule schedule, List<String> crewNicknames) {
        String crewList = crewNicknames.isEmpty() ? "Nenhum tripulante embarcado" : String.join(", ", crewNicknames);

        return EmbedCreateSpec.builder()
                .title("Escala: " + schedule.getTitle())
                .description("Escala de voo ativa")
                .addField("Aeronave", schedule.getAircraftType().getDisplayName(), true)
                .addField("Tipo de Missão", schedule.getMissionType().getDisplayName(), true)
                .addField("Subtipo de Ação", schedule.getActionSubType() != null ? schedule.getActionSubType().getDisplayName() : "N/A", true)
                .addField("Opção", schedule.getActionOption() != null ? schedule.getActionOption() : "N/A", true)
                .addField("Escalante", schedule.getCreatedByUsername(), true)
                .addField("Início", DATE_TIME_FORMATTER.format(schedule.getStartTime()), true)
                .addField("Status", schedule.isActive() ? "Ativa" : "Encerrada", true)
                .addField("Tripulantes", crewList, false)
                .color(getMissionColor(schedule.getMissionType()))
                .timestamp(Instant.now())
                .build();
    }

    private Color getMissionColor(MissionType missionType) {
        return switch (missionType) {
            case PATROL -> Color.BLUE;
            case ACTION -> Color.RED;
            default -> Color.BLUE;
        };
    }
}