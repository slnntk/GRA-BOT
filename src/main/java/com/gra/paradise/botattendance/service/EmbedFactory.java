package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.config.DiscordConfig;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.gra.paradise.botattendance.config.DiscordConfig.*;

@Component
@RequiredArgsConstructor
public class EmbedFactory {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("America/Sao_Paulo"));
    public static final String FOOTER_TEXT = "G.R.A - Controle Operacional | v1.0 | by Tiago Holanda";


    public EmbedCreateSpec createSystemOverviewEmbed() {
        return EmbedCreateSpec.builder()
                .image(FOOTER_CHOOSE_HELI_FIRST_SCREEN_URL)
                .title("🚁 Sistema de Escalas Águias")
                .description("Controle as operações dos helicópteros Águias da Polícia. 🚨\n**Pronto para gerenciar?** Clique no botão para iniciar.")
                .color(Color.of(0, 102, 204)) // Dark blue for authority
                .addField("📋 Instruções", """
                    • Clique em **Criar Escala** para registrar uma operação
                    • Selecione o **helicóptero**
                    • Escolha o **tipo de operação**
                    • Confirme os detalhes
                    """, false)
                .addField("🔔 Status Atual", "Nenhuma escala ativa. Inicie uma operação agora! 🚁", false)
                .footer(FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(ZonedDateTime.now(FORTALEZA_ZONE).toInstant())
                .build();
    }

    public EmbedCreateSpec createAircraftSelectionEmbed() {
        return EmbedCreateSpec.builder()
                .image(FOOTER_CHOOSE_HELI_FIRST_SCREEN_URL)
                .title("🚁 Selecione o Helicóptero")
                .description("Escolha o helicóptero para a operação.")
                .color(Color.of(0, 102, 204))
                .addField("💡 Dica", "Verifique a disponibilidade do helicóptero antes de selecionar.", false)
                .footer(FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(ZonedDateTime.now(FORTALEZA_ZONE).toInstant())
                .build();
    }

    public EmbedCreateSpec createMissionSelectionEmbed(AircraftType aircraftType) {
        String aircraftImageUrl = DiscordConfig.AIRCRAFT_IMAGE_URLS.getOrDefault(aircraftType, FOOTER_GRA_BLUE_URL);
        return EmbedCreateSpec.builder()
                .image(aircraftImageUrl)
                .title("🚨 Escolha o Tipo de Operação")
                .description("**Helicóptero**: " + aircraftType.getDisplayName() + "\nDefina o tipo de operação (ex.: Patrulha, Ação Tática ou Outros).")
                .color(Color.of(0, 102, 204))
                .addField("💡 Dica", "Patrulhamento refere-se às ocasiões em que o helicóptero está em operação nas ruas, independentemente de ser em prioridade, código 0 ou outras situações. Já ações são momentos em que a equipe GRA ou os operadores de combate atuam diretamente em intervenções táticas. Outros permite especificar uma missão personalizada.", false)
                .footer(FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(ZonedDateTime.now(FORTALEZA_ZONE).toInstant())
                .build();
    }

    public EmbedCreateSpec createActionSubTypeSelectionEmbed(AircraftType aircraftType) {
        String aircraftImageUrl = DiscordConfig.AIRCRAFT_IMAGE_URLS.getOrDefault(aircraftType, FOOTER_GRA_BLUE_URL);
        return EmbedCreateSpec.builder()
                .image(aircraftImageUrl)
                .title("⚙️ Defina o Subtipo de Operação")
                .description("**Helicóptero**: " + aircraftType.getDisplayName() + "\nEscolha o subtipo (Fuga, Tiro).")
                .color(Color.of(0, 102, 204))
                .addField("💡 Dica", "O subtipo define o tipo da operação. Escolha com precisão!", false)
                .footer(FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(ZonedDateTime.now(FORTALEZA_ZONE).toInstant())
                .build();
    }

    public EmbedCreateSpec createActionOptionSelectionEmbed(AircraftType aircraftType, ActionSubType subType) {
        String aircraftImageUrl = DiscordConfig.AIRCRAFT_IMAGE_URLS.getOrDefault(aircraftType, FOOTER_GRA_BLUE_URL);
        return EmbedCreateSpec.builder()
                .image(aircraftImageUrl)
                .title("✅ Finalize a Operação")
                .description("**Helicóptero**: " + aircraftType.getDisplayName() + "\n**Subtipo**: " + subType.getDisplayName() + "\nSelecione a ação (ex: Joalheria).")
                .color(Color.of(0, 102, 204))
                .addField("💡 Dica", "Confira todos os detalhes antes de finalizar!", false)
                .footer(FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(ZonedDateTime.now(FORTALEZA_ZONE).toInstant())
                .build();
    }

    public EmbedCreateSpec createScheduleConfirmationEmbed(AircraftType aircraftType, MissionType missionType, String title, ActionSubType actionSubType, String actionOption) {
        String aircraftImageUrl = DiscordConfig.AIRCRAFT_IMAGE_URLS.getOrDefault(aircraftType, FOOTER_GRA_BLUE_URL);
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                .image(aircraftImageUrl)
                .title("✅ Operação Confirmada!")
                .description("Escala operacional registrada com sucesso. Confira os detalhes: 📋")
                .addField("📌 Título", title != null ? title : "Operação sem título", true)
                .addField("🚁 Helicóptero", aircraftType.getDisplayName(), true)
                .addField("🚨 Operação", missionType.getDisplayName(), true);

        if (missionType == MissionType.OUTROS) {
            builder.addField("⚙️ Atividade", actionOption != null ? actionOption : "Não especificado", true);
        } else if (missionType == MissionType.ACTION && actionSubType != null && actionOption != null) {
            builder.addField("⚙️ Subtipo", actionSubType.getDisplayName(), true)
                    .addField("🔧 Opção", actionOption, true);
        } else {
            builder.addField("⚙️ Subtipo", "Não especificado", true)
                    .addField("🔧 Opção", "Não especificado", true);
        }

        return builder.color(Color.of(0, 153, 0)) // Green for confirmation
                .footer(FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(ZonedDateTime.now(FORTALEZA_ZONE).toInstant())
                .build();
    }

    public EmbedCreateSpec createSchedulePublicEmbed(Schedule schedule, List<String> crewNicknames) {
        List<String> updatedCrewNicknames = new ArrayList<>(crewNicknames);
        updatedCrewNicknames.add("**" + schedule.getCreatedByUsername() + "** 👨‍✈️");
        String crewList = updatedCrewNicknames.isEmpty() ? "Nenhum tripulante designado 🚶" : String.join("\n", updatedCrewNicknames);
        String aircraftImageUrl = DiscordConfig.AIRCRAFT_IMAGE_URLS.getOrDefault(schedule.getAircraftType(), FOOTER_GRA_BLUE_URL);
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                .image(aircraftImageUrl)
                .title("📅 Escala Ativa: " + schedule.getTitle())
                .description("Detalhes da operação! 🚁")
                .addField("🚁 Helicóptero", schedule.getAircraftType().getDisplayName(), true)
                .addField("🚨 Operação", schedule.getMissionType().getDisplayName(), true);

        if (schedule.getMissionType() == MissionType.OUTROS) {
            builder.addField("⚙️ Descrição", schedule.getOutrosDescription() != null ? schedule.getOutrosDescription() : "Não especificado", true);
        } else {
            builder.addField("⚙️ Subtipo", schedule.getActionSubType() != null ? schedule.getActionSubType().getDisplayName() : "Não especificado", true)
                    .addField("🔧 Opção", schedule.getActionOption() != null ? schedule.getActionOption() : "Não especificado", true);
        }

        return builder.addField("👨‍✈️ Piloto", "**" + schedule.getCreatedByUsername() + "**", true)
                .addField("📅 Início", DATE_TIME_FORMATTER.format(schedule.getStartTime()), true)
                .addField("🔄 Status", schedule.isActive() ? "Ativa   ✅" : "Encerrada   🛑", true)
                .addField("👥 Tripulação", crewList, false)
                .color(getMissionColor(schedule.getMissionType()))
                .footer(FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(ZonedDateTime.now(FORTALEZA_ZONE).toInstant())
                .build();
    }

    private Color getMissionColor(MissionType missionType) {
        return switch (missionType) {
            case PATROL -> Color.of(0, 102, 204); // Dark blue for patrol
            case ACTION -> Color.of(204, 0, 0); // Red for action
            case OUTROS -> Color.of(0, 153, 153); // Teal for outros
            default -> Color.of(0, 153, 153); // Teal for fallback
        };
    }
}