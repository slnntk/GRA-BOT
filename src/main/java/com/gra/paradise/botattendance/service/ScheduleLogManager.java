package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.config.DiscordConfig;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScheduleLogManager {

    private final ScheduleLogRepository scheduleLogRepository;
    private final GatewayDiscordClient discordClient;

    private final Map<MissionType, String> logChannelIds = new HashMap<>();
    private final Map<Long, String> scheduleLogMessages = new HashMap<>();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    // Initialize default channel IDs
    public ScheduleLogManager(ScheduleLogRepository scheduleLogRepository, GatewayDiscordClient discordClient) {
        this.scheduleLogRepository = scheduleLogRepository;
        this.discordClient = discordClient;
        logChannelIds.put(MissionType.ACTION, "1392491828237832332");
        logChannelIds.put(MissionType.PATROL, "1392491968218665030");
        log.info("Default log channels configured: ACTION -> {}, PATROL -> {}",
                logChannelIds.get(MissionType.ACTION), logChannelIds.get(MissionType.PATROL));
    }

    @Transactional
    public void createScheduleLog(Schedule schedule, String action, String userId, String username, String details) {
        ScheduleLog scheduleLog = new ScheduleLog(schedule, action, userId, username, details);
        scheduleLogRepository.save(scheduleLog);
        log.info("Log salvo: {} para escala {} por usuário {}", action, schedule.getId(), username);
    }

    public Mono<Void> sendScheduleCreationLog(Schedule schedule) {
        List<String> crewNicknames = schedule.getInitializedCrewMembers().stream()
                .map(User::getNickname)
                .collect(Collectors.toList());
        String crewList = crewNicknames.isEmpty() ? "Nenhum tripulante embarcado" : String.join(", ", crewNicknames);
        String aircraftImageUrl = DiscordConfig.AIRCRAFT_IMAGE_URLS.getOrDefault(schedule.getAircraftType(), DiscordConfig.FOOTER_GRA_BLUE_URL);

        // Save the creation log entry
        createScheduleLog(schedule, "CREATED", schedule.getCreatedById(), schedule.getCreatedByUsername(),
                "Escala criada: " + schedule.getTitle());

        EmbedCreateSpec logEmbed = EmbedCreateSpec.builder()
                .title("✅ Nova Escala Criada")
                .description("Uma nova escala de voo foi criada")
                .addField("Escala", schedule.getTitle(), true)
                .addField("Aeronave", schedule.getAircraftType().getDisplayName(), true)
                .addField("Tipo de Missão", schedule.getMissionType().getDisplayName(), true)
                .addField("Subtipo de Ação", schedule.getActionSubType() != null ? schedule.getActionSubType().getDisplayName() : "N/A", true)
                .addField("Opção", schedule.getActionOption() != null ? schedule.getActionOption() : "N/A", true)
                .addField("Piloto", schedule.getCreatedByUsername(), true)
                .addField("Início", DATE_TIME_FORMATTER.format(schedule.getStartTime()), true)
                .addField("Tripulação", crewList, false)
                .addField("Últimas atividades", formatActivity(
                        schedule.getCreatedByUsername() + ": Escala criada: " + schedule.getTitle(),
                        schedule.getStartTime()), false)
                .color(getMissionColor(schedule.getMissionType()))
                .footer("G.R.A Bot - Escala de Voo", aircraftImageUrl)
                .timestamp(Instant.now())
                .build();

        String channelId = getLogChannelId(schedule.getMissionType());
        if (channelId == null) {
            log.error("Canal de logs não configurado para a missão {}. Não é possível enviar log de criação para a escala {}",
                    schedule.getMissionType(), schedule.getId());
            return Mono.error(new IllegalStateException("Canal de logs não configurado"));
        }

        return sendLogEmbed(logEmbed, schedule.getMissionType())
                .doOnNext(message -> {
                    String messageId = message.getId().asString();
                    scheduleLogMessages.put(schedule.getId(), messageId);
                    log.info("Log de criação da escala {} registrado com mensagem ID {}", schedule.getId(), messageId);
                })
                .doOnError(e -> log.error("Erro ao enviar log de criação da escala {}: {}", schedule.getId(), e.getMessage(), e))
                .then();
    }

    public Mono<Void> updateScheduleLogMessage(Schedule schedule, String activityMessage) {
        String messageId = scheduleLogMessages.get(schedule.getId());
        String channelId = getLogChannelId(schedule.getMissionType());
        String aircraftImageUrl = DiscordConfig.AIRCRAFT_IMAGE_URLS.getOrDefault(schedule.getAircraftType(), DiscordConfig.FOOTER_GRA_BLUE_URL);

        if (messageId == null || channelId == null) {
            log.warn("Não foi possível atualizar log da escala {}. MessageId: {}, ChannelId: {}",
                    schedule.getId(), messageId, channelId);
            return sendScheduleCreationLog(schedule);
        }

        List<String> crewNicknames = schedule.getInitializedCrewMembers().stream()
                .map(User::getNickname)
                .collect(Collectors.toList());
        String crewList = crewNicknames.isEmpty() ? "Nenhum tripulante embarcado" : String.join(", ", crewNicknames);

        String activityHistory = getRecentLogs(schedule.getId()).stream()
                .map(log -> formatActivity(
                        log.getUsername() + ": " + log.getDetails(),
                        log.getTimestamp().atZone(ZoneId.of("America/Sao_Paulo")).toInstant()))
                .collect(Collectors.joining("\n"));

        EmbedCreateSpec updatedLogEmbed = EmbedCreateSpec.builder()
                .title("✈️ Escala em Andamento: " + schedule.getTitle())
                .description("Escala de voo ativa")
                .addField("Aeronave", schedule.getAircraftType().getDisplayName(), true)
                .addField("Tipo de Missão", schedule.getMissionType().getDisplayName(), true)
                .addField("Subtipo de Ação", schedule.getActionSubType() != null ? schedule.getActionSubType().getDisplayName() : "N/A", true)
                .addField("Opção", schedule.getActionOption() != null ? schedule.getActionOption() : "N/A", true)
                .addField("Piloto", schedule.getCreatedByUsername(), true)
                .addField("Início", DATE_TIME_FORMATTER.format(schedule.getStartTime()), true)
                .addField("Tripulação", crewList, false)
                .addField("Últimas Atividades", activityHistory.isEmpty() ? "Nenhuma atividade registrada" : activityHistory, false)
                .color(getMissionColor(schedule.getMissionType()))
                .footer("G.R.A Bot - Escala de Voo", aircraftImageUrl)
                .timestamp(Instant.now())
                .build();

        return discordClient.getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.getMessageById(Snowflake.of(messageId))
                        .flatMap(message -> message.edit(MessageEditSpec.builder()
                                .addEmbed(updatedLogEmbed)
                                .build()))
                        .onErrorResume(e -> {
                            log.warn("Erro ao atualizar mensagem de log {}: {}. Criando nova mensagem.", messageId, e.getMessage());
                            return sendLogEmbed(updatedLogEmbed, schedule.getMissionType())
                                    .doOnNext(newMessage -> scheduleLogMessages.put(schedule.getId(), newMessage.getId().asString()));
                        }))
                .then()
                .doOnSuccess(v -> log.info("Log da escala {} atualizado com sucesso", schedule.getId()))
                .doOnError(e -> log.error("Erro ao atualizar log da escala {}: {}", schedule.getId(), e.getMessage()));
    }

    public Mono<Void> createFinalScheduleLogMessage(Long scheduleId, String title, AircraftType aircraftType,
                                                    MissionType missionType, ActionSubType actionSubType,
                                                    String actionOption, Instant startTime,
                                                    Instant endTime, String pilotName, String closedByName,
                                                    List<String> crewNicknames) {
        String duration = formatDuration(startTime, endTime);
        String crewList = crewNicknames.isEmpty() ? "Nenhum tripulante embarcou nesta escala"
                : String.join(", ", crewNicknames);

        String activityHistory = getRecentLogs(scheduleId).stream()
                .map(log -> formatActivity(
                        log.getUsername() + ": " + log.getDetails(),
                        log.getTimestamp().atZone(ZoneId.of("America/Sao_Paulo")).toInstant()))
                .collect(Collectors.joining("\n"));

        EmbedCreateSpec finalLogEmbed = EmbedCreateSpec.builder()
                .title("🏁 Escala Encerrada: " + title)
                .description("Esta escala de voo foi concluída")
                .addField("Aeronave", aircraftType.getDisplayName(), true)
                .addField("Tipo de Missão", missionType.getDisplayName(), true)
                .addField("Subtipo de Ação", actionSubType != null ? actionSubType.getDisplayName() : "N/A", true)
                .addField("Opção", actionOption != null ? actionOption : "N/A", true)
                .addField("Piloto", pilotName, true)
                .addField("Duração Total", duration, true)
                .addField("Início", DATE_TIME_FORMATTER.format(startTime), true)
                .addField("Término", DATE_TIME_FORMATTER.format(endTime), true)
                .addField("Tripulantes e tempos de serviço", crewList, false)
                .addField("Histórico de Atividades", activityHistory.isEmpty() ? "Nenhuma atividade registrada" : activityHistory, false)
                .color(Color.RED)
                .footer("G.R.A Bot - Escala de Voo", DiscordConfig.FOOTER_GRA_BLUE_URL)
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
                            log.warn("Erro ao atualizar mensagem de log {}: {}. Criando nova mensagem.", messageId, e.getMessage());
                            return sendLogEmbed(finalLogEmbed, missionType)
                                    .doOnNext(newMessage -> scheduleLogMessages.put(scheduleId, newMessage.getId().asString()));
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
    public List<ScheduleLog> getRecentLogs(Long scheduleId) {
        List<ScheduleLog> logs = scheduleLogRepository.findByScheduleIdOrderByTimestampAsc(scheduleId);
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
            default -> Color.GREEN;
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
            return Mono.error(new IllegalStateException("Canal de logs não configurado"));
        }

        return discordClient.getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                        .addEmbed(embed)
                        .build()));
    }
}
