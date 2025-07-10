package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.model.Schedule;
import com.gra.paradise.botattendance.model.User;
import com.gra.paradise.botattendance.repository.ScheduleRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleMessageManager {

    private final GatewayDiscordClient discordClient;
    private final EmbedFactory embedFactory;
    private final ScheduleRepository scheduleRepository;

    private final Map<String, String> scheduleChannelMap = new HashMap<>();
    private final Map<String, String> scheduleMessageMap = new HashMap<>();
    private String systemChannelId;
    private String systemMessageId;

    public Mono<Void> registerScheduleMessage(String scheduleId, String channelId, String messageId) {
        scheduleChannelMap.put(scheduleId, channelId);
        scheduleMessageMap.put(scheduleId, messageId);
        log.info("Mensagem registrada para escala {}: canal {}, mensagem {}", scheduleId, channelId, messageId);
        return Mono.empty();
    }

    public Mono<Void> registerSystemMessage(String channelId, String messageId) {
        this.systemChannelId = channelId;
        this.systemMessageId = messageId;
        log.info("Mensagem de sistema registrada: canal {}, mensagem {}", channelId, messageId);
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
                .ofType(discord4j.core.object.entity.channel.MessageChannel.class)
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

    public Mono<Void> removeScheduleMessage(String scheduleId) {
        scheduleChannelMap.remove(scheduleId);
        scheduleMessageMap.remove(scheduleId);
        log.info("Mensagem removida para escala {}", scheduleId);
        return Mono.empty();
    }

    public Mono<Void> updateSystemMessage() {
        if (systemChannelId == null || systemMessageId == null) {
            log.warn("Mensagem de sistema não registrada para atualização.");
            return Mono.empty();
        }

        return discordClient.getChannelById(Snowflake.of(systemChannelId))
                .ofType(discord4j.core.object.entity.channel.MessageChannel.class)
                .flatMap(channel -> channel.getMessageById(Snowflake.of(systemMessageId)))
                .flatMap(message -> {
                    List<Schedule> activeSchedules = scheduleRepository.findByActiveTrue();
                    String activeSchedulesText = activeSchedules.isEmpty()
                            ? "Nenhuma escala ativa no momento"
                            : activeSchedules.stream()
                            .map(schedule -> {
                                List<String> crewNicknames = schedule.getInitializedCrewMembers().stream()
                                        .map(User::getNickname)
                                        .toList();
                                return String.format("**%s**: %s", schedule.getTitle(), crewNicknames.isEmpty() ? "Nenhum tripulante" : String.join(", ", crewNicknames));
                            })
                            .collect(Collectors.joining("\n"));

                    EmbedCreateSpec embed = EmbedCreateSpec.builder()
                            .title("Sistema de Escalas de Voo")
                            .description("Clique no botão abaixo para criar uma nova escala de voo.")
                            .addField("Instruções", "1. Clique em 'Criar Escala'\n2. Selecione a aeronave\n3. Selecione o tipo de missão\n4. Confirme", false)
                            .addField("Escalas Ativas", activeSchedulesText, false)
                            .timestamp(Instant.now())
                            .color(Color.BLUE)
                            .build();

                    Button createButton = Button.primary("create_schedule", "Criar Escala");

                    return message.edit()
                            .withEmbeds(embed)
                            .withComponents(ActionRow.of(createButton));
                })
                .doOnSuccess(v -> log.info("Mensagem de sistema atualizada com escalas ativas"))
                .doOnError(e -> log.error("Erro ao atualizar mensagem de sistema: {}", e.getMessage()))
                .then();
    }
}