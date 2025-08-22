package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.config.DiscordConfig;
import com.gra.paradise.botattendance.model.*;
import com.gra.paradise.botattendance.repository.GuildConfigRepository;
import com.gra.paradise.botattendance.repository.ScheduleLogRepository;
import com.gra.paradise.botattendance.repository.ScheduleRepository;
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
import org.hibernate.Hibernate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.stream.Collectors;

import static com.gra.paradise.botattendance.config.DiscordConfig.FORTALEZA_ZONE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleLogManager {

    private final ScheduleLogRepository scheduleLogRepository;
    private final ScheduleRepository scheduleRepository;
    private final GatewayDiscordClient discordClient;
    private final DiscordConfig discordConfig;
    private final GuildConfigRepository guildConfigRepository;

    private final Map<String, Map<Long, String>> scheduleLogMessages = new HashMap<>();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("America/Sao_Paulo"));
    private static final int MAX_FIELD_LENGTH = 1024; // Discord embed field character limit

    @Transactional
    @Async("taskExecutor") // Executar de forma assíncrona para não bloquear eventos Discord
    public void createScheduleLog(Schedule schedule, String action, String userId, String username, String details) {
        ScheduleLog scheduleLog = new ScheduleLog(schedule, action, userId, username, details);
        scheduleLogRepository.save(scheduleLog);
        log.info("Log salvo: {} para escala {} por usuário {} na guilda {}", action, schedule.getId(), username, schedule.getGuildId());
    }

    @Transactional
    public Mono<Void> sendScheduleCreationLog(String guildId, Schedule schedule) {
        Hibernate.initialize(schedule);
        Hibernate.initialize(schedule.getCrewMembers());
        schedule.initializeCrewMembers();
        
        // Otimizar stream processing e usar Optional.ofNullable para null safety
        String crewList = Optional.ofNullable(schedule.getInitializedCrewMembers())
                .map(crewMembers -> crewMembers.stream()
                        .map(User::getNickname)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", ")))
                .filter(result -> !result.isEmpty())
                .orElse("Nenhum tripulante embarcado");
                
        String aircraftImageUrl = DiscordConfig.AIRCRAFT_IMAGE_URLS.getOrDefault(schedule.getAircraftType(), DiscordConfig.FOOTER_GRA_BLUE_URL);

        createScheduleLog(schedule, "CREATED", schedule.getCreatedById(), schedule.getCreatedByUsername(),
                "Escala criada: " + schedule.getTitle());

        EmbedCreateSpec.Builder logEmbedBuilder = EmbedCreateSpec.builder()
                .title("✅ Nova Escala Criada")
                .description("Uma nova escala de voo foi criada")
                .addField("Escala", schedule.getTitle(), true)
                .addField("Aeronave", schedule.getAircraftType().getDisplayName(), true)
                .addField("Tipo de Missão", schedule.getMissionType().getDisplayName(), true)
                .addField(
                        schedule.getOutrosDescription() != null ? "Motivo" : "Subtipo de Ação",
                        schedule.getOutrosDescription() != null
                                ? schedule.getOutrosDescription()
                                : (schedule.getActionSubType() != null ? schedule.getActionSubType().getDisplayName() : "N/A"),
                        true
                )
                .addField("Opção", schedule.getActionOption() != null ? schedule.getActionOption() : "N/A", true)
                .addField("Piloto", schedule.getCreatedByUsername(), true)
                .addField("Início", DATE_TIME_FORMATTER.format(schedule.getStartTime()), true)
                .addField("Tripulação", crewList, false)
                .color(schedule.getOutrosDescription() != null ? Color.DISCORD_WHITE : Color.GREEN)
                .footer(EmbedFactory.FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(ZonedDateTime.now(FORTALEZA_ZONE).toInstant());

        // Add initial activity
        String initialActivity = formatActivity(
                schedule.getCreatedByUsername() + ": Escala criada: " + schedule.getTitle(),
                schedule.getStartTime());
        logEmbedBuilder.addField("Últimas atividades", initialActivity, false);

        String channelId = discordConfig.getLogChannelId(guildId, schedule.getMissionType());
        if (channelId == null) {
            log.error("Canal de logs não configurado para guilda {} e missão {}. Use /setup-log-channel.", guildId, schedule.getMissionType());
            return Mono.empty();
        }

        return sendLogEmbed(guildId, logEmbedBuilder.build(), schedule.getMissionType())
                .doOnNext(message -> {
                    String messageId = message.getId().asString();
                    scheduleLogMessages.computeIfAbsent(guildId, k -> new HashMap<>()).put(schedule.getId(), messageId);
                    log.info("Log de criação da escala {} registrado com mensagem ID {} na guilda {}", schedule.getId(), messageId, guildId);
                })
                .doOnError(e -> log.error("Erro ao enviar log de criação da escala {} na guilda {}: {}", schedule.getId(), guildId, e.getMessage()))
                .then();
    }

    @Transactional
    public Mono<Void> updateScheduleLogMessage(String guildId, Schedule schedule, String activityMessage) {
        Hibernate.initialize(schedule);
        Hibernate.initialize(schedule.getCrewMembers());
        schedule.initializeCrewMembers();
        Map<Long, String> guildMessages = scheduleLogMessages.getOrDefault(guildId, new HashMap<>());
        String messageId = guildMessages.get(schedule.getId());
        String channelId = discordConfig.getLogChannelId(guildId, schedule.getMissionType());

        if (messageId == null || channelId == null) {
            log.warn("Não foi possível atualizar log da escala {}. MessageId: {}, ChannelId: {}", schedule.getId(), messageId, channelId);
            return sendScheduleCreationLog(guildId, schedule);
        }

        List<String> crewNicknames = schedule.getInitializedCrewMembers().stream()
                .map(User::getNickname)
                .filter(nickname -> nickname != null)
                .collect(Collectors.toList());
        String crewList = crewNicknames.isEmpty() ? "Nenhum tripulante embarcado" : String.join(", ", crewNicknames);

        List<String> activityHistoryChunks = splitActivityHistory(getRecentLogs(schedule.getId()));
        log.info("Histórico de atividades para escala {}: {}", schedule.getId(), String.join("\n", activityHistoryChunks));

        EmbedCreateSpec.Builder updatedLogEmbedBuilder = EmbedCreateSpec.builder()
                .title("✈️ Escala em Andamento: " + schedule.getTitle())
                .description("Escala de voo ativa")
                .addField("Aeronave", schedule.getAircraftType().getDisplayName(), true)
                .addField(
                        schedule.getOutrosDescription() != null ? "Motivo" : "Subtipo de Ação",
                        schedule.getOutrosDescription() != null
                                ? schedule.getOutrosDescription()
                                : (schedule.getActionSubType() != null ? schedule.getActionSubType().getDisplayName() : "N/A"),
                        true
                )
                .addField("Subtipo de Ação", schedule.getActionSubType() != null ? schedule.getActionSubType().getDisplayName() : "N/A", true)
                .addField("Opção", schedule.getActionOption() != null ? schedule.getActionOption() : "N/A", true)
                .addField("Piloto", schedule.getCreatedByUsername(), true)
                .addField("Início", DATE_TIME_FORMATTER.format(schedule.getStartTime()), true)
                .addField("Tripulação", crewList, false)
                .color(Color.GREEN)
                .footer(EmbedFactory.FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(ZonedDateTime.now(FORTALEZA_ZONE).toInstant());

        // Add activity history chunks as separate fields
        for (int i = 0; i < activityHistoryChunks.size(); i++) {
            String fieldTitle = i == 0 ? "Últimas Atividades" : "Últimas Atividades (continuação " + (i + 1) + ")";
            updatedLogEmbedBuilder.addField(fieldTitle, activityHistoryChunks.get(i).isEmpty() ? "Nenhuma atividade registrada" : activityHistoryChunks.get(i), false);
        }

        return discordClient.getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.getMessageById(Snowflake.of(messageId))
                        .flatMap(message -> message.edit(MessageEditSpec.builder()
                                .addEmbed(updatedLogEmbedBuilder.build())
                                .build()))
                        .onErrorResume(e -> {
                            log.warn("Erro ao atualizar mensagem de log {}: {}. Criando nova mensagem.", messageId, e.getMessage());
                            return sendLogEmbed(guildId, updatedLogEmbedBuilder.build(), schedule.getMissionType())
                                    .doOnNext(newMessage -> scheduleLogMessages.computeIfAbsent(guildId, k -> new HashMap<>()).put(schedule.getId(), newMessage.getId().asString()));
                        }))
                .then()
                .doOnSuccess(v -> log.info("Log da escala {} atualizado com sucesso na guilda {}", schedule.getId(), guildId))
                .doOnError(e -> log.error("Erro ao atualizar log da escala {} na guilda {}: {}", schedule.getId(), guildId, e.getMessage()));
    }

    @Transactional
    public Mono<Void> createFinalScheduleLogMessage(String guildId, Long scheduleId, String title, AircraftType aircraftType,
                                                    MissionType missionType, ActionSubType actionSubType,
                                                    String actionOption, Instant startTime,
                                                    Instant endTime, String pilotName, String closedByName,
                                                    List<String> crewNicknames) {
        String duration = formatDuration(startTime, endTime);
        String crewList = crewNicknames.isEmpty() ? "Nenhum tripulante embarcou nesta escala"
                : String.join(", ", crewNicknames);

        List<String> activityHistoryChunks = splitActivityHistory(getRecentLogs(scheduleId));
        log.info("Histórico de atividades para escala final {}: {}", scheduleId, String.join("\n", activityHistoryChunks));

        Schedule schedule = scheduleRepository.findByIdAndGuildId(scheduleId, guildId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule não encontrado: " + scheduleId));
        Hibernate.initialize(schedule);
        Hibernate.initialize(schedule.getCrewMembers());
        schedule.initializeCrewMembers();

        EmbedCreateSpec.Builder finalLogEmbedBuilder = EmbedCreateSpec.builder()
                .title("🏁 Escala Encerrada: " + title)
                .description("Esta escala de voo foi concluída")
                .addField("Aeronave", aircraftType.getDisplayName(), true)
                .addField("Tipo de Missão", missionType.getDisplayName(), true)
                .addField(
                        schedule.getOutrosDescription() != null ? "Motivo" : "Subtipo de Ação",
                        schedule.getOutrosDescription() != null
                                ? schedule.getOutrosDescription()
                                : (schedule.getActionSubType() != null ? schedule.getActionSubType().getDisplayName() : "N/A"),
                        true
                )
                .addField("Opção", actionOption != null ? actionOption : "N/A", true)
                .addField("Piloto", pilotName, true)
                .addField("Duração Total", duration, true)
                .addField("Início", DATE_TIME_FORMATTER.format(startTime), true)
                .addField("Término", DATE_TIME_FORMATTER.format(endTime), true)
                .addField("Tripulantes", crewList, false)
                .color(Color.RED)
                .footer(EmbedFactory.FOOTER_TEXT, DiscordConfig.GRA_IMAGE_URL)
                .timestamp(ZonedDateTime.now(FORTALEZA_ZONE).toInstant());

        // Add activity history chunks as separate fields
        for (int i = 0; i < activityHistoryChunks.size(); i++) {
            String fieldTitle = i == 0 ? "Histórico de Atividades" : "Histórico de Atividades (continuação " + (i + 1) + ")";
            finalLogEmbedBuilder.addField(fieldTitle, activityHistoryChunks.get(i).isEmpty() ? "Nenhuma atividade registrada" : activityHistoryChunks.get(i), false);
        }

        Map<Long, String> guildMessages = scheduleLogMessages.getOrDefault(guildId, new HashMap<>());
        String messageId = guildMessages.get(scheduleId);
        String channelId = discordConfig.getLogChannelId(guildId, missionType);

        Mono<Void> sendOrUpdateLog = (messageId != null && channelId != null)
                ? discordClient.getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.getMessageById(Snowflake.of(messageId))
                        .flatMap(message -> message.edit(MessageEditSpec.builder()
                                .addEmbed(finalLogEmbedBuilder.build())
                                .build()))
                        .onErrorResume(e -> {
                            log.warn("Erro ao atualizar mensagem de log {}: {}. Criando nova mensagem.", messageId, e.getMessage());
                            return sendLogEmbed(guildId, finalLogEmbedBuilder.build(), missionType)
                                    .doOnNext(newMessage -> scheduleLogMessages.computeIfAbsent(guildId, k -> new HashMap<>()).put(scheduleId, newMessage.getId().asString()));
                        })
                        .then())
                : (channelId != null)
                ? sendLogEmbed(guildId, finalLogEmbedBuilder.build(), missionType)
                .doOnNext(message -> scheduleLogMessages.computeIfAbsent(guildId, k -> new HashMap<>()).put(scheduleId, message.getId().asString()))
                .then()
                : Mono.empty();

        return sendOrUpdateLog
                .doOnSuccess(v -> log.info("Log final da escala {} enviado com sucesso na guilda {}", scheduleId, guildId))
                .doOnError(e -> log.error("Falha ao enviar log final da escala {} na guilda {}: {}", scheduleId, guildId, e.getMessage()));
    }

    @Transactional(readOnly = true)
    public List<ScheduleLog> getRecentLogs(Long scheduleId) {
        List<ScheduleLog> logs = scheduleLogRepository.findByScheduleIdOrderByTimestampAsc(scheduleId);
        for (ScheduleLog log : logs) {
            Hibernate.initialize(log.getSchedule());
            Hibernate.initialize(log.getSchedule().getCrewMembers());
            log.getSchedule().initializeCrewMembers();
        }
        log.info("Logs encontrados para escala {}: {}", scheduleId, logs);
        return logs; // Return all logs without truncation
    }

    private List<String> splitActivityHistory(List<ScheduleLog> logs) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentLength = 0;

        for (ScheduleLog log : logs) {
            String activity = formatActivity(
                    log.getUsername() + ": " + log.getDetails().trim(),
                    log.getTimestamp().atZone(ZoneId.of("America/Sao_Paulo")).toInstant());
            int activityLength = activity.length() + 1; // +1 for newline

            if (currentLength + activityLength > MAX_FIELD_LENGTH) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                    currentLength = 0;
                }
            }

            currentChunk.append(activity).append("\n");
            currentLength += activityLength;
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
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
            case OUTROS -> Color.GREEN;
        };
    }

    @Transactional
    public Mono<Void> configureLogChannel(String guildId, String channelId, MissionType missionType) {
        return Mono.fromCallable(() -> {
            GuildConfig config = guildConfigRepository.findById(guildId)
                    .orElse(new GuildConfig());
            config.setGuildId(guildId);
            if (missionType == MissionType.ACTION) {
                config.setActionLogChannelId(channelId);
            } else if (missionType == MissionType.PATROL) {
                config.setPatrolLogChannelId(channelId);
            } else if (missionType == MissionType.OUTROS) {
                config.setOutrosLogChannelId(channelId);
            }
            guildConfigRepository.save(config);
            log.info("Canal de logs configurado para guilda {} e missão {}: {}", guildId, missionType, channelId);
            return config;
        }).flatMap(config -> {
            String confirmMessage = "✅ Sistema de logs de escalas configurado com sucesso para missões do tipo " + missionType.getDisplayName() + ".";
            return sendLogMessage(guildId, confirmMessage, missionType);
        });
    }

    private Mono<Void> sendLogMessage(String guildId, String content, MissionType missionType) {
        String channelId = discordConfig.getLogChannelId(guildId, missionType);
        if (channelId == null || discordClient == null) {
            log.error("Canal de logs não configurado para guilda {} e missão {}.", guildId, missionType);
            return Mono.empty();
        }

        return discordClient.getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(content))
                .then();
    }

    private Mono<Message> sendLogEmbed(String guildId, EmbedCreateSpec embed, MissionType missionType) {
        String channelId = discordConfig.getLogChannelId(guildId, missionType);
        if (channelId == null || discordClient == null) {
            log.error("Canal de logs não configurado para guilda {} e missão {}.", guildId, missionType);
            return Mono.empty();
        }

        return discordClient.getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                        .addEmbed(embed)
                        .build()));
    }
}
