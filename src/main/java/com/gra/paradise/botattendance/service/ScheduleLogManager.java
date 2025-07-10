package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.model.*;
import com.gra.paradise.botattendance.repository.ScheduleLogRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.util.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleLogManager {

    private final ScheduleLogRepository scheduleLogRepository;
    private final GatewayDiscordClient discordClient;

    private final Map<MissionType, String> logChannelIds = new HashMap<>();
    private final Map<Long, String> scheduleLogMessages = new HashMap<>();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    @Transactional
    public void createScheduleLog(Schedule schedule, String action, String userId, String username, String details) {
        ScheduleLog scheduleLog = new ScheduleLog(schedule, action, userId, username, details);
        scheduleLogRepository.save(scheduleLog);
    }

    public Mono<Void> sendScheduleCreationLog(Schedule schedule) {
        List<String> crewNicknames = schedule.getInitializedCrewMembers().stream()
                .map(User::getNickname)
                .collect(Collectors.toList());
        String crewList = crewNicknames.isEmpty() ? "Nenhum tripulante embarcado" : String.join(", ", crewNicknames);

        EmbedCreateSpec logEmbed = EmbedCreateSpec.builder()
                .title("✅ Nova Escala Criada")
                .description("Uma nova escala de voo foi criada")
                .addField("Escala", schedule.getTitle(), true)
                .addField("Aeronave", schedule.getAircraftType().getDisplayName(), true)
                .addField("Tipo de Missão", schedule.getMissionType().getDisplayName(), true)
                .addField("Subtipo de Ação", schedule.getActionSubType() != null ? schedule.getActionSubType().getDisplayName() : "N/A", true)
                .addField("Opção", schedule.getActionOption() != null ? schedule.getActionOption() : "N/A", true)
                .addField("Escalante", schedule.getCreatedByUsername(), true)
                .addField("Início", DATE_TIME_FORMATTER.format(schedule.getStartTime()), true)
                .addField("Tripulação", crewList, false)
                .addField("Últimas atividades", formatActivity(
                        schedule.getCreatedByUsername() + ": Escala criada com aeronave " +
                                schedule.getAircraftType().getDisplayName() + " para missão de " +
                                schedule.getMissionType().getDisplayName() +
                                (schedule.getActionSubType() != null ? " (" + schedule.getActionSubType().getDisplayName() + ": " + schedule.getActionOption() + ")" : ""),
                        schedule.getStartTime()), false)
                .color(getMissionColor(schedule.getMissionType()))
                .timestamp(Instant.now())
                .build();

        return sendLogEmbed(logEmbed, schedule.getMissionType())
                .flatMap(message -> {
                    scheduleLogMessages.put(schedule.getId(), message.getId().asString());
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error("Erro ao enviar log de criação da escala {}: {}", schedule.getId(), e.getMessage());
                    return Mono.empty();
                }).then();
    }

    public Mono<Void> updateScheduleLogMessage(Schedule schedule, String activityMessage) {
        Long scheduleId = schedule.getId();
        String title = schedule.getTitle();
        AircraftType aircraftType = schedule.getAircraftType();
        MissionType missionType = schedule.getMissionType();
        ActionSubType actionSubType = schedule.getActionSubType();
        String actionOption = schedule.getActionOption();
        Instant startTime = schedule.getStartTime();
        String pilotName = schedule.getCreatedByUsername();

        List<String> crewNicknames = schedule.getInitializedCrewMembers().stream()
                .map(User::getNickname)
                .collect(Collectors.toList());
        String crewList = crewNicknames.isEmpty() ? "Nenhum tripulante embarcado" : String.join(", ", crewNicknames);

        List<ScheduleLog> recentLogs = getRecentLogs(scheduleId);
        StringBuilder activityLog = new StringBuilder();
        for (ScheduleLog scheduleLog : recentLogs) {
            activityLog.append(DATE_TIME_FORMATTER.format(scheduleLog.getTimestamp()))
                    .append(" - ")
                    .append(scheduleLog.getUsername())
                    .append(": ")
                    .append(scheduleLog.getDetails())
                    .append("\n");
        }

        EmbedCreateSpec updatedLogEmbed = EmbedCreateSpec.builder()
                .title("📋 Escala: " + title)
                .description("Escala de voo ativa")
                .addField("Aeronave", aircraftType.getDisplayName(), true)
                .addField("Tipo de Missão", missionType.getDisplayName(), true)
                .addField("Subtipo de Ação", actionSubType != null ? actionSubType.getDisplayName() : "N/A", true)
                .addField("Opção", actionOption != null ? actionOption : "N/A", true)
                .addField("Escalante", pilotName, true)
                .addField("Início", DATE_TIME_FORMATTER.format(startTime), true)
                .addField("Duração", formatDuration(startTime, Instant.now()), true)
                .addField("Status", "Ativa", true)
                .addField("Tripulação", crewList, false)
                .addField("Últimas atividades", activityLog.toString(), false)
                .color(getMissionColor(missionType))
                .timestamp(Instant.now())
                .build();

        String messageId = scheduleLogMessages.get(scheduleId);
        String channelId = getLogChannelId(missionType);

        if (messageId != null && channelId != null) {
            return discordClient.getChannelById(Snowflake.of(channelId))
                    .ofType(MessageChannel.class)
                    .flatMap(channel -> channel.getMessageById(Snowflake.of(messageId)))
                    .flatMap(message -> message.edit(MessageEditSpec.builder()
                            .addEmbed(updatedLogEmbed)
                            .build()))
                    .onErrorResume(e -> {
                        log.error("Erro ao atualizar log da escala {}: {}", scheduleId, e.getMessage());
                        return Mono.empty();
                    })
                    .then();
        } else if (channelId != null) {
            return sendLogEmbed(updatedLogEmbed, missionType)
                    .flatMap(message -> {
                        scheduleLogMessages.put(scheduleId, message.getId().asString());
                        return Mono.empty();
                    })
                    .onErrorResume(e -> {
                        log.error("Erro ao criar novo log da escala {}: {}", scheduleId, e.getMessage());
                        return Mono.empty();
                    }).then();
        }
        return Mono.empty();
    }

    public Mono<Void> createFinalScheduleLogMessage(Long scheduleId, String title, AircraftType aircraftType,
                                                    MissionType missionType, ActionSubType actionSubType,
                                                    String actionOption, Instant startTime,
                                                    Instant endTime, String pilotName, String closedByName,
                                                    List<String> crewNicknames) {
        String duration = formatDuration(startTime, endTime);
        String crewList = crewNicknames.isEmpty() ? "Nenhum tripulante embarcou nesta escala"
                : String.join(", ", crewNicknames);
        String activityHistory = getActivityHistory(scheduleId);

        EmbedCreateSpec finalLogEmbed = EmbedCreateSpec.builder()
                .title("🏁 Escala Encerrada: " + title)
                .description("Esta escala de voo foi concluída")
                .addField("Aeronave", aircraftType.getDisplayName(), true)
                .addField("Tipo de Missão", missionType.getDisplayName(), true)
                .addField("Subtipo de Ação", actionSubType != null ? actionSubType.getDisplayName() : "N/A", true)
                .addField("Opção", actionOption != null ? actionOption : "N/A", true)
                .addField("Escalante", pilotName, true)
                .addField("Início", DATE_TIME_FORMATTER.format(startTime), true)
                .addField("Término", DATE_TIME_FORMATTER.format(endTime), true)
                .addField("Duração Total", duration, true)
                .addField("Tripulantes e tempos de serviço", crewList, false)
                .addField("Histórico de Atividades", activityHistory, false)
                .color(Color.RED)
                .timestamp(Instant.now())
                .build();

        String messageId = scheduleLogMessages.get(scheduleId);
        String channelId = getLogChannelId(missionType);

        Mono<Void> sendOrUpdateLog = (messageId != null && channelId != null)
                ? discordClient.getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.getMessageById(Snowflake.of(messageId))
                        .flatMap(message -> message.edit(MessageEditSpec.builder()
                                .addEmbed(finalLogEmbed)
                                .build()))
                        .onErrorResume(e -> {
                            if (e.getMessage() != null && e.getMessage().contains("Unknown Message")) {
                                log.warn("Mensagem de log {} não encontrada. Criando nova mensagem de log.", messageId);
                                return sendLogEmbed(finalLogEmbed, missionType)
                                        .doOnNext(newMessage -> scheduleLogMessages.put(scheduleId, newMessage.getId().asString()));
                            }
                            log.error("Erro ao atualizar log final da escala {}: {}", scheduleId, e.getMessage());
                            return Mono.error(e);
                        })
                        .then())
                : (channelId != null)
                ? sendLogEmbed(finalLogEmbed, missionType)
                .doOnNext(message -> scheduleLogMessages.put(scheduleId, message.getId().asString()))
                .then()
                : Mono.error(new IllegalStateException("Canal de logs não configurado para a missão " + missionType));

        return sendOrUpdateLog
                .doOnSuccess(v -> log.info("Log final da escala {} enviado com sucesso", scheduleId))
                .doOnError(e -> log.error("Falha ao enviar log final da escala {}: {}", scheduleId, e.getMessage()));
    }

    @Transactional(readOnly = true)
    public String getActivityHistory(Long scheduleId) {
        List<ScheduleLog> logs = scheduleLogRepository.findByScheduleIdOrderByTimestampDesc(scheduleId);
        if (logs.isEmpty()) {
            return "Nenhuma atividade registrada";
        }

        StringBuilder history = new StringBuilder();
        for (ScheduleLog scheduleLog : logs) {
            history.append(DATE_TIME_FORMATTER.format(scheduleLog.getTimestamp()))
                    .append(" - ")
                    .append(scheduleLog.getUsername())
                    .append(": ")
                    .append(scheduleLog.getDetails())
                    .append("\n");
        }
        return history.toString();
    }

    @Transactional(readOnly = true)
    public List<ScheduleLog> getRecentLogs(Long scheduleId) {
        List<ScheduleLog> logs = scheduleLogRepository.findByScheduleIdOrderByTimestampDesc(scheduleId);
        if (logs.size() > 5) {
            logs = logs.subList(0, 5);
        }
        return logs;
    }

    private String formatDateTime(Instant instant) {
        return DATE_TIME_FORMATTER.format(instant);
    }

    private String formatActivity(String message, Instant time) {
        return formatDateTime(time) + " - " + message;
    }

    private String formatDuration(Instant start, Instant end) {
        long minutes = java.time.Duration.between(start, end).toMinutes();
        long hours = minutes / 60;
        minutes = minutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    private Color getMissionColor(MissionType missionType) {
        return switch (missionType) {
            case PATROL -> Color.BLUE;
            case ACTION -> Color.RED;
            default -> Color.BLUE;
        };
    }

    public Mono<Void> configureLogChannel(String channelId, MissionType missionType) {
        if (missionType == null) {
            for (MissionType type : MissionType.values()) {
                this.logChannelIds.put(type, channelId);
            }
            log.info("Canal de logs configurado para todos os tipos de missão: {}", channelId);
        } else {
            this.logChannelIds.put(missionType, channelId);
            log.info("Canal de logs configurado para missão {}: {}", missionType, channelId);
        }

        String confirmMessage = "✅ Sistema de logs de escalas configurado com sucesso para " +
                (missionType == null ? "todos os tipos de missão" : "missões do tipo " + missionType.getDisplayName()) + ".";

        return sendLogMessage(confirmMessage, missionType);
    }

    private Mono<Void> sendLogMessage(String content, MissionType missionType) {
        String channelId = getLogChannelId(missionType);
        if (channelId == null || discordClient == null) {
            return Mono.empty();
        }

        return discordClient.getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(content))
                .then();
    }

    public String getLogChannelId(MissionType missionType) {
        if (missionType != null && logChannelIds.containsKey(missionType)) {
            return logChannelIds.get(missionType);
        }
        return logChannelIds.getOrDefault(MissionType.PATROL, null);
    }

    private Mono<Message> sendLogEmbed(EmbedCreateSpec embed, MissionType missionType) {
        String channelId = getLogChannelId(missionType);
        if (channelId == null || discordClient == null) {
            return Mono.empty();
        }

        return discordClient.getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                        .addEmbed(embed)
                        .build()));
    }
}