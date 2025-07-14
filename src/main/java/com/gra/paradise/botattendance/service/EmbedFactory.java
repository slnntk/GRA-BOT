package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.config.DiscordConfig;
import com.gra.paradise.botattendance.model.*;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EmbedFactory {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Sao_Paulo"));
    public static final String FOOTER_TEXT = "G.R.A Bot - Controle Operacional | v1.0";

    // Creates the system overview embed with a "Start" button
    public MessageCreateSpec createSystemOverviewMessage() {
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .thumbnail(DiscordConfig.GRA_IMAGE_URL)
                .title("🚁 Sistema de Escalas Águias")
                .description("Bem-vindo ao controle operacional dos Águias! 🚨\n**Pronto para gerenciar?**")
                .color(Color.of(0, 102, 204)) // Dark blue
                .addField("📋 Instruções", """
                    • Clique em **Iniciar** para criar uma nova escala
                    • Siga os passos para selecionar helicóptero e operação
                    • Confirme os detalhes no final
                    """, false)
                .addField("🔔 Status", "Nenhuma escala ativa. Crie uma agora! 🚁", false)
                .footer(FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(Instant.now())
                .build();

        Button startButton = Button.primary("start_operation", "Iniciar Operação");
        return MessageCreateSpec.builder()
                .addEmbed(embed)
                .addComponent(ActionRow.of(startButton))
                .build();
    }

    // Creates embed for aircraft selection with a select menu
    public MessageCreateSpec createAircraftSelectionEmbed() {
        List<SelectMenu.Option> aircraftOptions = List.of(AircraftType.values())
                .stream()
                .map(aircraft -> SelectMenu.Option.of(aircraft.getDisplayName(), aircraft.name()))
                .collect(Collectors.toList());

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .thumbnail(DiscordConfig.FOOTER_GRA_BLUE_URL)
                .title("🚁 Selecione o Helicóptero")
                .description("Escolha o helicóptero para a operação.")
                .color(Color.of(0, 102, 204))
                .addField("💡 Dica", "Verifique a disponibilidade antes de selecionar.", false)
                .footer(FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(Instant.now())
                .build();

        SelectMenu aircraftMenu = SelectMenu.of("select_aircraft", aircraftOptions)
                .withPlaceholder("Selecione um helicóptero");
        Button cancelButton = Button.danger("cancel_operation", "Cancelar");

        return MessageCreateSpec.builder()
                .addEmbed(embed)
                .addComponent(ActionRow.of(aircraftMenu))
                .addComponent(ActionRow.of(cancelButton))
                .build();
    }

    // Creates embed for mission type selection with a select menu
    public MessageCreateSpec createMissionSelectionEmbed(AircraftType aircraftType) {
        String aircraftImageUrl = DiscordConfig.AIRCRAFT_IMAGE_URLS.getOrDefault(aircraftType, DiscordConfig.FOOTER_GRA_BLUE_URL);
        List<SelectMenu.Option> missionOptions = List.of(MissionType.values())
                .stream()
                .map(mission -> SelectMenu.Option.of(mission.getDisplayName(), mission.name()))
                .collect(Collectors.toList());

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .thumbnail(aircraftImageUrl)
                .title("🚨 Escolha o Tipo de Operação")
                .description("**Helicóptero**: " + aircraftType.getDisplayName() + "\nDefina o tipo de operação (ex.: Patrulha ou Ação Tática).")
                .color(Color.of(0, 102, 204))
                .addField("💡 Dica", "Patrulhamento refere-se às ocasiões em que o helicóptero está em operação nas ruas. Já ações são momentos em que a equipe atua diretamente em intervenções táticas.", false)
                .footer(FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(Instant.now())
                .build();

        SelectMenu missionMenu = SelectMenu.of("select_mission", missionOptions)
                .withPlaceholder("Selecione o tipo de operação");
        Button backButton = Button.secondary("back_to_aircraft", "Voltar");
        Button cancelButton = Button.danger("cancel_operation", "Cancelar");

        return MessageCreateSpec.builder()
                .addEmbed(embed)
                .addComponent(ActionRow.of(missionMenu))
                .addComponent(ActionRow.of(backButton, cancelButton))
                .build();
    }

    // Creates embed for action subtype selection with a select menu
    public MessageCreateSpec createActionSubTypeSelectionEmbed(AircraftType aircraftType) {
        String aircraftImageUrl = DiscordConfig.AIRCRAFT_IMAGE_URLS.getOrDefault(aircraftType, DiscordConfig.FOOTER_GRA_BLUE_URL);
        List<SelectMenu.Option> subTypeOptions = List.of(ActionSubType.values())
                .stream()
                .map(subType -> SelectMenu.Option.of(subType.getDisplayName(), subType.name()))
                .collect(Collectors.toList());

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .thumbnail(aircraftImageUrl)
                .title("⚙️ Defina o Subtipo de Operação")
                .description("**Helicóptero**: " + aircraftType.getDisplayName() + "\nEscolha o subtipo (Fuga, Tiro).")
                .color(Color.of(0, 102, 204))
                .addField("💡 Dica", "O subtipo define o tipo da operação. Escolha com precisão!", false)
                .footer(FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(Instant.now())
                .build();

        SelectMenu subTypeMenu = SelectMenu.of("select_subtype", subTypeOptions)
                .withPlaceholder("Selecione o subtipo");
        Button backButton = Button.secondary("back_to_mission", "Voltar");
        Button cancelButton = Button.danger("cancel_operation", "Cancelar");

        return MessageCreateSpec.builder()
                .addEmbed(embed)
                .addComponent(ActionRow.of(subTypeMenu))
                .addComponent(ActionRow.of(backButton, cancelButton))
                .build();
    }

    // Creates embed for action option selection with buttons
    public MessageCreateSpec createActionOptionSelectionEmbed(AircraftType aircraftType, ActionSubType subType) {
        String aircraftImageUrl = DiscordConfig.AIRCRAFT_IMAGE_URLS.getOrDefault(aircraftType, DiscordConfig.FOOTER_GRA_BLUE_URL);
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .thumbnail(aircraftImageUrl)
                .title("✅ Finalize a Operação")
                .description("**Helicóptero**: " + aircraftType.getDisplayName() + "\n**Subtipo**: " + subType.getDisplayName() + "\nSelecione a ação (ex: Joalheria).")
                .color(Color.of(0, 102, 204))
                .addField("💡 Dica", "Confira todos os detalhes antes de finalizar!", false)
                .footer(FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(Instant.now())
                .build();

        Button joalheriaButton = Button.primary("action_joalheria", "Joalheria");
        Button bancoButton = Button.primary("action_banco", "Banco");
        Button backButton = Button.secondary("back_to_subtype", "Voltar");
        Button cancelButton = Button.danger("cancel_operation", "Cancelar");

        return MessageCreateSpec.builder()
                .addEmbed(embed)
                .addComponent(ActionRow.of(joalheriaButton, bancoButton))
                .addComponent(ActionRow.of(backButton, cancelButton))
                .build();
    }

    // Creates confirmation embed with confirmation buttons
    public MessageCreateSpec createScheduleConfirmationEmbed(AircraftType aircraftType, MissionType missionType, String title, ActionSubType actionSubType, String actionOption) {
        String aircraftImageUrl = DiscordConfig.AIRCRAFT_IMAGE_URLS.getOrDefault(aircraftType, DiscordConfig.FOOTER_GRA_BLUE_URL);
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                .thumbnail(aircraftImageUrl)
                .title("✅ Operação Confirmada!")
                .description("Escala operacional registrada com sucesso. Confira os detalhes: 📋")
                .addField("📌 Título", title != null ? title : "Operação sem título", true)
                .addField("🚁 Helicóptero", aircraftType.getDisplayName(), true)
                .addField("🚨 Operação", missionType.getDisplayName(), true);

        if (actionSubType != null && actionOption != null) {
            builder.addField("⚙️ Subtipo", actionSubType.getDisplayName(), true)
                    .addField("🔧 Opção", actionOption, true);
        } else {
            builder.addField("⚙️ Subtipo", "Não especificado", true)
                    .addField("🔧 Opção", "Não especificado", true);
        }

        EmbedCreateSpec embed = builder.color(Color.of(0, 153, 0)) // Green for confirmation
                .footer(FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(Instant.now())
                .build();

        Button confirmButton = Button.success("confirm_schedule", "Confirmar");
        Button editButton = Button.secondary("edit_schedule", "Editar");
        Button cancelButton = Button.danger("cancel_operation", "Cancelar");

        return MessageCreateSpec.builder()
                .addEmbed(embed)
                .addComponent(ActionRow.of(confirmButton, editButton, cancelButton))
                .build();
    }

    // Creates public schedule embed without components
    public EmbedCreateSpec createSchedulePublicEmbed(Schedule schedule, List<String> crewNicknames) {
        String crewList = schedule.getInitializedCrewMembers().isEmpty()
                ? "Nenhum tripulante designado 🚶"
                : schedule.getInitializedCrewMembers().stream()
                .map(User::getUsername)
                .collect(Collectors.joining(", "));
        String aircraftImageUrl = DiscordConfig.AIRCRAFT_IMAGE_URLS.getOrDefault(schedule.getAircraftType(), DiscordConfig.FOOTER_GRA_BLUE_URL);

        return EmbedCreateSpec.builder()
                .thumbnail(aircraftImageUrl)
                .title("📅 Escala Ativa: " + schedule.getTitle())
                .description("Detalhes da operação em andamento dos Águias! 🚁")
                .color(getMissionColor(schedule.getMissionType()))
                .addField("🚁 Helicóptero", schedule.getAircraftType().getDisplayName(), true)
                .addField("🚨 Operação", schedule.getMissionType().getDisplayName(), true)
                .addField("⚙️ Subtipo", schedule.getActionSubType() != null ? schedule.getActionSubType().getDisplayName() : "Não especificado", true)
                .addField("🔧 Opção", schedule.getActionOption() != null ? schedule.getActionOption() : "Não especificado", true)
                .addField("👨‍✈️ Piloto", schedule.getCreatedByUsername(), true)
                .addField("📅 Início", DATE_TIME_FORMATTER.format(schedule.getStartTime()), true)
                .addField("🔄 Status", schedule.isActive() ? "Ativa ✅" : "Encerrada 🛑", true)
                .addField("👥 Tripulação", crewList, false)
                .footer(FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(Instant.now())
                .build();
    }

    private Color getMissionColor(MissionType missionType) {
        return switch (missionType) {
            case PATROL -> Color.of(0, 102, 204); // Dark blue for patrol
            case ACTION -> Color.of(204, 0, 0); // Red for action
            default -> Color.of(0, 153, 153); // Teal for fallback
        };
    }
}