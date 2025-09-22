package com.gra.paradise.botattendance.discord.buttons;

import com.gra.paradise.botattendance.exception.*;
import com.gra.paradise.botattendance.model.Schedule;
import com.gra.paradise.botattendance.service.EmbedFactory;
import com.gra.paradise.botattendance.service.ScheduleLogManager;
import com.gra.paradise.botattendance.service.ScheduleManager;
import com.gra.paradise.botattendance.service.ScheduleMessageManager;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionCallbackSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "discord.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleActionHandler {

    private final ScheduleManager scheduleManager;
    private final EmbedFactory embedFactory;
    private final ScheduleMessageManager scheduleMessageManager;
    private final ScheduleLogManager scheduleLogManager;
    private final GatewayDiscordClient gateway;


    private static final String BOARD_PREFIX = "board_schedule:";
    private static final String LEAVE_PREFIX = "leave_schedule:";
    private static final String END_PREFIX = "end_schedule:";
    private static final String ERROR_INVALID_ID = "❌ Erro: ID da escala inválido ou malformado";
    private static final String ERROR_UNEXPECTED = "❌ Erro inesperado ao %s. Tente novamente ou contate o suporte.";

    @Transactional
    public Mono<Void> handleBoardSchedule(ButtonInteractionEvent event) {
        return handleScheduleAction(event, BOARD_PREFIX, "Embarcar", "embarcou");
    }

    @Transactional
    public Mono<Void> handleLeaveSchedule(ButtonInteractionEvent event) {
        return handleScheduleAction(event, LEAVE_PREFIX, "Desembarcar", "desembarcou");
    }

    @Transactional
    public Mono<Void> handleEndSchedule(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith(END_PREFIX)) {
            log.warn("CustomId inválido para encerrar escala: {}", customId);
            return replyError(event, ERROR_INVALID_ID);
        }

        Long scheduleId = parseScheduleId(customId, END_PREFIX);
        if (scheduleId == null) return replyError(event, ERROR_INVALID_ID);

        String guildId = getGuildId(event);
        String userId = getUserId(event);
        String username = getUsername(event);

        return event.deferReply(InteractionCallbackSpec.builder().ephemeral(true).build())
                .then(Mono.fromCallable(() -> scheduleManager.findByIdAndGuildId(scheduleId, guildId).orElse(null))
                        .subscribeOn(Schedulers.boundedElastic()))
                .flatMap(schedule -> {
                    if (schedule == null || !schedule.isActive()) {
                        log.warn("Escala {} inexistente ou inativa.", scheduleId);
                        return disableButtonsAndRemoveMessage(event, scheduleId, guildId)
                                .then(event.editReply("❌ A escala com ID " + scheduleId + " não está disponível."));
                    }

                    return Mono.justOrEmpty(event.getInteraction().getMember())
                            .map(m -> m.getNickname().orElse(username))
                            .switchIfEmpty(Mono.just(username))
                            .flatMap(nickname ->
                                    processEndSchedule(guildId, scheduleId, userId, nickname)
                                            .flatMap(result ->
                                                    disableButtonsAndRemoveMessage(event, scheduleId, guildId)
                                                            .then(scheduleLogManager.createFinalScheduleLogMessage(
                                                                    guildId, result.schedule.getId(), result.schedule.getTitle(),
                                                                    result.schedule.getAircraftType(), result.schedule.getMissionType(),
                                                                    result.schedule.getActionSubType(), result.schedule.getActionOption(),
                                                                    result.schedule.getStartTime(), result.schedule.getEndTime(),
                                                                    result.schedule.getCreatedByUsername(), nickname, result.crewNicknames))
                                                            .then(scheduleMessageManager.updateSystemMessage(guildId))
                                                            .then(event.editReply("Escala encerrada com sucesso!"))
                                            )
                            );
                })
                .onErrorResume(e -> {
                    log.error("Erro ao encerrar escala {}: {}", scheduleId, e.getMessage(), e);
                    if (e instanceof OnlyCreatorCanCloseScheduleException) {
                        return event.editReply("❌ Somente o criador pode encerrar a escala.");
                    } else if (e instanceof ScheduleAlreadyClosedException) {
                        return event.editReply("❌ Esta escala já foi encerrada.");
                    } else if (e instanceof ScheduleNotFoundException) {
                        return event.editReply("❌ Escala não encontrada.");
                    } else {
                        return event.editReply(String.format(ERROR_UNEXPECTED, "encerrar a escala"));
                    }
                })
                .then();
    }


    private Mono<ScheduleActionResult> processEndSchedule(String guildId, Long scheduleId, String userId, String nickname) {
        return Mono.fromCallable(() -> scheduleManager.closeSchedule(guildId, scheduleId, userId, nickname))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(schedule -> getCrewNicknamesWithLiveLookup(schedule, gateway, guildId)
                        .map(nicknames -> new ScheduleActionResult(schedule, nicknames)));
    }

    private Mono<Void> handleScheduleAction(ButtonInteractionEvent event, String prefix, String action, String logAction) {
        String customId = event.getCustomId();
        if (!customId.startsWith(prefix)) {
            log.warn("CustomId inválido para {}: {}", action, customId);
            return replyError(event, ERROR_INVALID_ID);
        }

        Long scheduleId = parseScheduleId(customId, prefix);
        if (scheduleId == null) return replyError(event, ERROR_INVALID_ID);

        String guildId = getGuildId(event);
        String userId = getUserId(event);
        String username = getUsername(event);

        return event.deferReply(InteractionCallbackSpec.builder().ephemeral(true).build())
                .then(
                        Mono.justOrEmpty(event.getInteraction().getMember())
                                .flatMap(member -> Mono.justOrEmpty(member.getNickname())
                                        .defaultIfEmpty(member.getUsername()))
                                .defaultIfEmpty(username)
                )
                .flatMap(nickname -> Mono.fromCallable(() -> {
                            Schedule schedule = scheduleManager.findByIdAndGuildId(scheduleId, guildId)
                                    .orElseThrow();

                            if (!schedule.isActive()) throw new ScheduleAlreadyClosedException();


                            return prefix.equals(BOARD_PREFIX)
                                            ? scheduleManager.addCrewMember(guildId, scheduleId, userId, username, nickname)
                                            : scheduleManager.removeCrewMember(guildId, scheduleId, userId, nickname);
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(schedule ->
                                        getCrewNicknamesWithLiveLookup(schedule, gateway, guildId)
                                                .flatMap(nicknames -> {
                                                    EmbedCreateSpec embed = embedFactory.createSchedulePublicEmbed(schedule, nicknames);

                                                    ActionRow buttons = ActionRow.of(
                                                            Button.success(BOARD_PREFIX + schedule.getId(), "Embarcar").disabled(!schedule.isActive()),
                                                            Button.danger(LEAVE_PREFIX + schedule.getId(), "Desembarcar").disabled(!schedule.isActive()),
                                                            Button.secondary(END_PREFIX + schedule.getId(), "Encerrar Escala").disabled(!schedule.isActive())
                                                    );

                                                    return scheduleLogManager.updateScheduleLogMessage(guildId, schedule, "Usuário " + nickname + " " + logAction)
                                                            .then(scheduleMessageManager.updateScheduleMessage(String.valueOf(schedule.getId()), nicknames))
                                                            .then(Mono.justOrEmpty(event.getMessage())
                                                                    .flatMap(m -> m.edit().withEmbeds(embed).withComponents(buttons))
                                                                    .onErrorResume(e -> {
                                                                        log.warn("Erro ao atualizar mensagem: {}", e.getMessage());
                                                                        return Mono.empty();
                                                                    }))
                                                            .then(event.editReply(action + " realizado com sucesso!"));
                                                })
                                )
                )
                .onErrorResume(e -> {
                    log.error("Erro ao realizar {} na escala {}: {}", action, scheduleId, e.getMessage(), e);

                    if (e instanceof UserNotBoardedException) {
                        return event.editReply("❌ Você não está embarcado nesta escala.");
                    } else if (e instanceof CreatorCannotLeaveException) {
                        return event.editReply("❌ O criador da escala não pode desembarcar.");
                    } else if (e instanceof IllegalStateException && e.getMessage().contains("Criador")) {
                        return event.editReply("❌ O criador da escala não pode desembarcar.");
                    } else if (e instanceof IllegalStateException && e.getMessage().contains("encerrar")) {
                        return event.editReply("❌ Somente o criador pode encerrar a escala.");
                    } else if (e instanceof OnlyCreatorCanCloseScheduleException) {
                        return event.editReply("❌ Somente o criador pode encerrar a escala.");
                    } else if (e instanceof PilotCannotBeCrewException) {
                        return event.editReply("❌ O piloto não pode ser tripulante.");
                    } else if (e instanceof UserAlreadyBoardedException) {
                        return event.editReply("❌ Você já está embarcado nesta escala.");
                    } else {
                        return event.editReply(String.format(ERROR_UNEXPECTED, action.toLowerCase()));
                    }
                })
                .then();
    }


    private Long parseScheduleId(String customId, String prefix) {
        try {
            return Long.parseLong(customId.substring(prefix.length()));
        } catch (Exception e) {
            log.warn("Erro ao parsear ID: {}", customId);
            return null;
        }
    }

    private String getGuildId(ButtonInteractionEvent event) {
        return event.getInteraction().getGuildId().map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("Comando deve ser usado em servidor"));
    }

    private String getUserId(ButtonInteractionEvent event) {
        return event.getInteraction().getUser().getId().asString();
    }

    private String getUsername(ButtonInteractionEvent event) {
        return event.getInteraction().getUser().getUsername();
    }

    private Mono<Void> disableButtonsAndRemoveMessage(ButtonInteractionEvent event, Long scheduleId, String guildId) {
        return Mono.justOrEmpty(scheduleManager.findByIdAndGuildId(scheduleId, guildId))
                .flatMap(schedule -> Mono.justOrEmpty(schedule.getChannelId())
                        .flatMap(channelId -> Mono.justOrEmpty(schedule.getMessageId())
                                .flatMap(messageId -> event.getClient().getChannelById(Snowflake.of(channelId))
                                        .ofType(MessageChannel.class)
                                        .flatMap(channel -> channel.getMessageById(Snowflake.of(messageId))
                                                .flatMap(message -> message.delete()
                                                        .then(Mono.empty())
                                                )
                                        )
                                        .onErrorResume(e -> {
                                            log.warn("Erro ao excluir mensagem da escala {}: {}", scheduleId, e.getMessage());
                                            return Mono.empty();
                                        })
                                )
                        )
                )
                .then(scheduleMessageManager.removeScheduleMessage(String.valueOf(scheduleId), guildId))
                .onErrorResume(e -> {
                    log.warn("Erro ao remover referência da escala {}: {}", scheduleId, e.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<List<String>> getCrewNicknamesWithLiveLookup(Schedule schedule, GatewayDiscordClient gateway, String guildId) {
        if (schedule.getInitializedCrewMembers() == null || schedule.getInitializedCrewMembers().isEmpty()) {
            return Mono.just(Collections.emptyList());
        }

        Snowflake guildSnowflake = Snowflake.of(guildId);

        List<Mono<String>> nicknameMonos = schedule.getInitializedCrewMembers().stream()
                .map(user -> gateway.getGuildById(guildSnowflake)
                        .flatMap(guild -> guild.getMemberById(Snowflake.of(user.getDiscordId())))
                        .map(member -> member.getNickname().orElse(member.getUsername())) // extrai nickname ou username do Member
                        .defaultIfEmpty(Optional.ofNullable(user.getNickname()).orElse(user.getUsername())) // caso Member não encontrado
                        .onErrorResume(e -> Mono.just(Optional.ofNullable(user.getNickname()).orElse(user.getUsername()))) // fallback em erro
                )
                .toList();

        return Flux.merge(nicknameMonos).collectList();
    }




    private Mono<Void> replyError(ButtonInteractionEvent event, String message) {
        return event.createFollowup(message).withEphemeral(true).then();
    }

    private record ScheduleActionResult(Schedule schedule, List<String> crewNicknames) {}
}