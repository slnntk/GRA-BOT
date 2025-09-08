package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.config.DiscordConfig;
import com.gra.paradise.botattendance.model.MissionType;
import com.gra.paradise.botattendance.model.Schedule;
import com.gra.paradise.botattendance.model.SystemMessage;
import com.gra.paradise.botattendance.repository.ScheduleRepository;
import com.gra.paradise.botattendance.repository.SystemMessageRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.gra.paradise.botattendance.config.DiscordConfig.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleMessageManager {

    private final GatewayDiscordClient discordClient;
    private final EmbedFactory embedFactory;
    private final ScheduleRepository scheduleRepository;
    private final SystemMessageRepository systemMessageRepository;
    private final DiscordConfig discordConfig;

    private final Map<String, String> scheduleChannelMap = new HashMap<>(); // scheduleId -> channelId
    private final Map<String, String> scheduleMessageMap = new HashMap<>(); // scheduleId -> messageId
    private final Map<String, String> systemChannelMap = new HashMap<>(); // guildId -> channelId
    private final Map<String, String> systemMessageMap = new HashMap<>(); // guildId -> messageId

    @PostConstruct
    public void initializeSystemMessages() {
        systemMessageRepository.findAll().forEach(systemMessage -> {
            String guildId = systemMessage.getGuildId();
            systemChannelMap.put(guildId, systemMessage.getChannelId());
            systemMessageMap.put(guildId, systemMessage.getMessageId());
            log.info("Mensagem do sistema carregada do banco para guilda {}: canal {}, mensagem {}", guildId, systemMessage.getChannelId(), systemMessage.getMessageId());
            verifySystemMessage(guildId).subscribe();
        });
    }

    private Mono<Void> verifySystemMessage(String guildId) {
        String systemChannelId = systemChannelMap.get(guildId);
        String systemMessageId = systemMessageMap.get(guildId);

        if (systemChannelId == null || systemMessageId == null) {
            log.warn("IDs da mensagem do sistema não disponíveis para guilda {}. Criando nova mensagem.", guildId);
            return createSystemMessage(guildId);
        }

        return discordClient.getChannelById(Snowflake.of(systemChannelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.getMessageById(Snowflake.of(systemMessageId))
                        .then(Mono.empty())
                        .onErrorResume(e -> {
                            log.warn("Mensagem do sistema não encontrada para guilda {}: {}. Criando nova mensagem.", guildId, systemMessageId);
                            return createSystemMessage(guildId);
                        })).then();
    }

    public Mono<Void> createSystemMessage(String guildId) {
        String defaultChannelId = discordConfig.getDefaultSystemChannelId(guildId);
        if (defaultChannelId == null) {
            log.error("Canal padrão não configurado para guilda {}. Use /setup-escala para configurar.", guildId);
            // Return empty instead of trying to use guildId as channelId
            return Mono.empty();
        }

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .image(FOOTER_GRA_BLUE_URL)
                .title("🚁 Sistema de Escalas G.R.A")
                .description("Bem-vindo ao controle operacional da G.R.A! 🚨\n**Pronto para gerenciar?**")
                .color(Color.of(0, 102, 204))
                .addField("📋 Instruções", """
                        • Clique em **Iniciar** para criar uma nova escala
                        • Siga os passos para selecionar helicóptero e operação
                        • Confirme os detalhes no final
                        """, false)
                .addField("🔔 Status", "Nenhuma escala ativa. Crie uma agora! 🚁", false)
                .footer(EmbedFactory.FOOTER_TEXT, GRA_IMAGE_URL)
                .timestamp(ZonedDateTime.now(FORTALEZA_ZONE).toInstant())
                .build();

        Button createButton = Button.primary("create_schedule", "Iniciar Escala");

        return discordClient.getChannelById(Snowflake.of(defaultChannelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage()
                        .withEmbeds(embed)
                        .withComponents(ActionRow.of(createButton))
                        .flatMap(message -> {
                            String messageId = message.getId().asString();
                            systemChannelMap.put(guildId, defaultChannelId);
                            systemMessageMap.put(guildId, messageId);
                            SystemMessage systemMessage = new SystemMessage();
                            systemMessage.setGuildId(guildId);
                            systemMessage.setChannelId(defaultChannelId);
                            systemMessage.setMessageId(messageId);
                            systemMessageRepository.save(systemMessage);
                            log.info("Nova mensagem do sistema criada para guilda {}: canal {}, mensagem {}", guildId, defaultChannelId, messageId);
                            return Mono.empty();
                        }))
                .doOnError(e -> log.error("Erro ao criar mensagem do sistema para guilda {}: {}", guildId, e.getMessage()))
                .then();
    }

    public Mono<Void> registerScheduleMessage(String scheduleId, String channelId, String messageId) {
        scheduleChannelMap.put(scheduleId, channelId);
        scheduleMessageMap.put(scheduleId, messageId);
        log.info("Mensagem registrada para escala {}: canal {}, mensagem {}", scheduleId, channelId, messageId);
        return Mono.empty();
    }

    public Mono<Void> registerSystemMessage(String guildId, String channelId, String messageId) {
        systemChannelMap.put(guildId, channelId);
        systemMessageMap.put(guildId, messageId);
        SystemMessage systemMessage = new SystemMessage();
        systemMessage.setGuildId(guildId);
        systemMessage.setChannelId(channelId);
        systemMessage.setMessageId(messageId);
        systemMessageRepository.save(systemMessage);
        log.info("Mensagem de sistema registrada para guilda {}: canal {}, mensagem {}", guildId, channelId, messageId);
        return Mono.empty();
    }

    public Mono<Void> updateScheduleMessage(String scheduleId, List<String> crewNicknames) {
        String channelId = scheduleChannelMap.get(scheduleId);
        String messageId = scheduleMessageMap.get(scheduleId);

        if (channelId == null || messageId == null) {
            log.error("Não foi possível encontrar canal ou mensagem para a escala {}", scheduleId);
            return Mono.empty();
        }

        return discordClient.getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.getMessageById(Snowflake.of(messageId)))
                .flatMap(message -> {
                    Schedule schedule = scheduleRepository.findById(Long.parseLong(scheduleId))
                            .orElseThrow(() -> new IllegalStateException("Escala não encontrada: " + scheduleId));
                    EmbedCreateSpec embed = embedFactory.createSchedulePublicEmbed(schedule, crewNicknames);

                    Button boardButton = Button.success("board_schedule:" + scheduleId, "Embarcar")
                            .disabled(!schedule.isActive());
                    Button leaveButton = Button.danger("leave_schedule:" + scheduleId, "Desembarcar")
                            .disabled(!schedule.isActive());
                    Button endButton = Button.secondary("end_schedule:" + scheduleId, "Encerrar Escala")
                            .disabled(!schedule.isActive());

                    return message.edit()
                            .withEmbeds(embed)
                            .withComponents(ActionRow.of(boardButton, leaveButton, endButton));
                })
                .doOnSuccess(v -> log.info("Mensagem da escala {} atualizada com tripulantes: {}", scheduleId, crewNicknames))
                .doOnError(e -> log.error("Erro ao atualizar mensagem da escala {}: {}", scheduleId, e.getMessage()))
                .then();
    }

    public Mono<Void> removeScheduleMessage(String scheduleId, String guildId) {
        String channelId = scheduleChannelMap.get(scheduleId);
        String messageId = scheduleMessageMap.get(scheduleId);

        if (channelId == null || messageId == null) {
            log.warn("Nenhum canal ou mensagem encontrado para a escala {}. Não é possível excluir a mensagem.", scheduleId);
            scheduleChannelMap.remove(scheduleId);
            scheduleMessageMap.remove(scheduleId);
            return updateSystemMessage(guildId);
        }

        return discordClient.getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.getMessageById(Snowflake.of(messageId))
                        .flatMap(message -> message.delete("Escala encerrada"))
                        .doOnSuccess(v -> log.info("Mensagem da escala {} excluída do canal {}", scheduleId, channelId))
                        .doOnError(e -> log.error("Erro ao excluir mensagem da escala {}: {}", scheduleId, e.getMessage())))
                .then(Mono.fromRunnable(() -> {
                    scheduleChannelMap.remove(scheduleId);
                    scheduleMessageMap.remove(scheduleId);
                    log.info("Registros de mensagem removidos para escala {}", scheduleId);
                }))
                .then(updateSystemMessage(guildId));
    }

    public Mono<Void> updateSystemMessage(String guildId) {
        String systemChannelId = systemChannelMap.get(guildId);
        String systemMessageId = systemMessageMap.get(guildId);

        if (systemChannelId == null || systemMessageId == null) {
            log.warn("Mensagem de sistema não registrada para guilda {}. Tentando recriar.", guildId);
            return createSystemMessage(guildId);
        }

        return discordClient.getChannelById(Snowflake.of(systemChannelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.getMessageById(Snowflake.of(systemMessageId)))
                .flatMap(message -> {
                    List<Schedule> activeSchedules = scheduleRepository.findByActiveTrueAndGuildId(guildId);
                    String statusMessage;
                    if (activeSchedules.isEmpty()) {
                        statusMessage = "Nenhuma escala ativa. Crie uma agora! 🚁";
                    } else {
                        statusMessage = activeSchedules.stream()
                                .map(schedule -> String.format("**%s**: Piloto %s (%s)",
                                        schedule.getTitle(),
                                        schedule.getCreatedByUsername(),
                                        schedule.getMissionType() == MissionType.OUTROS
                                                ? schedule.getOutrosDescription()
                                                : schedule.getMissionType() == MissionType.ACTION
                                                ? schedule.getActionOption()
                                                : schedule.getMissionType().getDisplayName()
                                ))
                                .collect(Collectors.joining("\n"));
                    }

                    EmbedCreateSpec embed = EmbedCreateSpec.builder()
                            .image(FOOTER_GRA_BLUE_URL)
                            .title("🚁 Sistema de Escalas G.R.A")
                            .description("Bem-vindo ao controle operacional da G.R.A! 🚨\n**Pronto para gerenciar?**")
                            .color(Color.of(0, 102, 204))
                            .addField("📋 Instruções", """
                                    • Clique em **Iniciar** para criar uma nova escala
                                    • Siga os passos para selecionar helicóptero e operação
                                    • Confirme os detalhes no final
                                    """, false)
                            .addField("🔔 Status", statusMessage, false)
                            .footer(EmbedFactory.FOOTER_TEXT, GRA_IMAGE_URL)
                            .timestamp(ZonedDateTime.now(FORTALEZA_ZONE).toInstant())
                            .build();

                    Button createButton = Button.primary("create_schedule", "Iniciar Escala");

                    return message.edit()
                            .withEmbeds(embed)
                            .withComponents(ActionRow.of(createButton));
                })
                .doOnSuccess(v -> log.info("Mensagem de sistema atualizada com escalas ativas para guilda {}", guildId))
                .doOnError(e -> {
                    log.error("Erro ao atualizar mensagem do sistema para guilda {}: {}. Tentando recriar.", guildId, e.getMessage());
                    createSystemMessage(guildId).subscribe();
                })
                .then();
    }

    public Mono<Map<String, String>> getScheduleMessageDetails(String scheduleId) {
        return Mono.justOrEmpty(scheduleChannelMap.get(scheduleId))
                .map(channelId -> {
                    String messageId = scheduleMessageMap.get(scheduleId);
                    Map<String, String> details = new HashMap<>();
                    details.put("channelId", channelId);
                    details.put("messageId", messageId != null ? messageId : "");
                    log.debug("Detalhes da mensagem recuperados para escala {}: {}", scheduleId, details);
                    return details;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Nenhum detalhe de mensagem encontrado para a escala {}", scheduleId);
                    return Mono.empty();
                }));
    }
}