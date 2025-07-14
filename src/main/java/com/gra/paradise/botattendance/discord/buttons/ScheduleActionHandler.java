package com.gra.paradise.botattendance.discord.buttons;

import com.gra.paradise.botattendance.model.Schedule;
import com.gra.paradise.botattendance.model.User;
import com.gra.paradise.botattendance.service.EmbedFactory;
import com.gra.paradise.botattendance.service.ScheduleLogManager;
import com.gra.paradise.botattendance.service.ScheduleManager;
import com.gra.paradise.botattendance.service.ScheduleMessageManager;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionCallbackSpec;
import discord4j.rest.http.client.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleActionHandler {

    private final ScheduleManager scheduleManager;
    private final EmbedFactory embedFactory;
    private final ScheduleMessageManager scheduleMessageManager;
    private final ScheduleLogManager scheduleLogManager;

    public Mono<Void> handleBoardSchedule(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("board_schedule:")) {
            log.error("CustomId inválido para embarcar: {}. Esperado prefixo 'board_schedule:'", customId);
            return event.createFollowup("❌ Erro: ID da escala inválido").withEphemeral(true).then();
        }

        Long scheduleId;
        try {
            scheduleId = Long.parseLong(customId.substring("board_schedule:".length()));
            if (scheduleId <= 0) {
                throw new NumberFormatException("ID inválido");
            }
        } catch (NumberFormatException e) {
            log.error("Erro ao parsear scheduleId de customId '{}': {}", customId, e.getMessage(), e);
            return event.createFollowup("❌ Erro: ID da escala inválido ou malformado").withEphemeral(true).then();
        }

        String userId = event.getInteraction().getUser().getId().asString();
        String username = event.getInteraction().getUser().getUsername();

        return event.deferReply(InteractionCallbackSpec.builder().ephemeral(true).build())
                .then(event.deleteReply())
                .then(Mono.justOrEmpty(event.getInteraction().getMember())
                        .flatMap(member -> {
                            String nickname = member.getNickname().orElse(username);
                            log.info("Usuário {} ({}) embarcando na escala {} com apelido: {}", username, userId, scheduleId, nickname);

                            return Mono.fromCallable(() -> {
                                Schedule updatedSchedule = scheduleManager.addCrewMemberAndInitialize(scheduleId, userId, username, nickname);
                                List<String> crewNicknames = getCrewNicknames(updatedSchedule);
                                log.info("Crew nicknames para escala {}: {}", scheduleId, crewNicknames);
                                return new ScheduleActionResult(updatedSchedule, crewNicknames);
                            });
                        })
                        .switchIfEmpty(Mono.fromCallable(() -> {
                            log.info("Usando username padrão para usuário {} ({}) na escala {}", username, userId, scheduleId);
                            Schedule updatedSchedule = scheduleManager.addCrewMemberAndInitialize(scheduleId, userId, username, username);
                            List<String> crewNicknames = getCrewNicknames(updatedSchedule);
                            log.info("Crew nicknames para escala {}: {}", scheduleId, crewNicknames);
                            return new ScheduleActionResult(updatedSchedule, crewNicknames);
                        })))
                .flatMap(result -> {
                    Schedule updatedSchedule = result.schedule;
                    List<String> crewNicknames = result.crewNicknames != null ? result.crewNicknames : Collections.emptyList();
                    EmbedCreateSpec embed = embedFactory.createSchedulePublicEmbed(updatedSchedule, crewNicknames);

                    Button boardButton = Button.success("board_schedule:" + updatedSchedule.getId(), "Embarcar")
                            .disabled(!updatedSchedule.isActive());
                    Button leaveButton = Button.danger("leave_schedule:" + updatedSchedule.getId(), "Desembarcar")
                            .disabled(!updatedSchedule.isActive());
                    Button endButton = Button.secondary("end_schedule:" + updatedSchedule.getId(), "Encerrar Escala")
                            .disabled(!updatedSchedule.isActive());

                    return scheduleLogManager.updateScheduleLogMessage(updatedSchedule, "Usuário " + username + " embarcou")
                            .then(scheduleMessageManager.updateScheduleMessage(String.valueOf(updatedSchedule.getId()), crewNicknames))
                            .then(Mono.justOrEmpty(event.getMessage())
                                    .flatMap(message -> message.edit()
                                            .withEmbeds(embed)
                                            .withComponents(ActionRow.of(boardButton, leaveButton, endButton)))
                                    .switchIfEmpty(Mono.empty())
                                    .then());
                })
                .onErrorResume(e -> {
                    log.error("Erro ao processar botão board_schedule para escala {}: {}", scheduleId, e.getMessage(), e);
                    String errorMessage = e instanceof IllegalArgumentException || e instanceof IllegalStateException
                            ? e.getMessage()
                            : "Erro inesperado ao embarcar. Tente novamente ou contate o suporte.";
                    return event.createFollowup("❌ " + errorMessage).withEphemeral(true).then();
                });
    }

    public Mono<Void> handleLeaveSchedule(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("leave_schedule:")) {
            log.error("CustomId inválido para desembarcar: {}. Esperado prefixo 'leave_schedule:'", customId);
            return event.createFollowup("❌ Erro: ID da escala inválido").withEphemeral(true).then();
        }

        Long scheduleId;
        try {
            scheduleId = Long.parseLong(customId.substring("leave_schedule:".length()));
            if (scheduleId <= 0) {
                throw new NumberFormatException("ID inválido");
            }
        } catch (NumberFormatException e) {
            log.error("Erro ao parsear scheduleId de customId '{}': {}", customId, e.getMessage(), e);
            return event.createFollowup("❌ Erro: ID da escala inválido ou malformado").withEphemeral(true).then();
        }

        String userId = event.getInteraction().getUser().getId().asString();
        String username = event.getInteraction().getUser().getUsername();

        return event.deferReply(InteractionCallbackSpec.builder().ephemeral(true).build())
                .then(event.deleteReply())
                .then(Mono.justOrEmpty(event.getInteraction().getMember())
                        .flatMap(member -> {
                            String nickname = member.getNickname().orElse(username);
                            log.info("Usuário {} ({}) desembarcando da escala {} com apelido: {}", username, userId, scheduleId, nickname);

                            return Mono.fromCallable(() -> {
                                Schedule updatedSchedule = scheduleManager.removeCrewMemberAndInitialize(scheduleId, userId, nickname);
                                List<String> crewNicknames = getCrewNicknames(updatedSchedule);
                                log.info("Crew nicknames para escala {}: {}", scheduleId, crewNicknames);
                                return new ScheduleActionResult(updatedSchedule, crewNicknames);
                            });
                        })
                        .switchIfEmpty(Mono.fromCallable(() -> {
                            log.info("Usando username padrão para usuário {} ({}) na escala {}", username, userId, scheduleId);
                            Schedule updatedSchedule = scheduleManager.removeCrewMemberAndInitialize(scheduleId, userId, username);
                            List<String> crewNicknames = getCrewNicknames(updatedSchedule);
                            log.info("Crew nicknames para escala {}: {}", scheduleId, crewNicknames);
                            return new ScheduleActionResult(updatedSchedule, crewNicknames);
                        })))
                .flatMap(result -> {
                    Schedule updatedSchedule = result.schedule;
                    List<String> crewNicknames = result.crewNicknames != null ? result.crewNicknames : Collections.emptyList();
                    EmbedCreateSpec embed = embedFactory.createSchedulePublicEmbed(updatedSchedule, crewNicknames);

                    Button boardButton = Button.success("board_schedule:" + updatedSchedule.getId(), "Embarcar")
                            .disabled(!updatedSchedule.isActive());
                    Button leaveButton = Button.danger("leave_schedule:" + updatedSchedule.getId(), "Desembarcar")
                            .disabled(!updatedSchedule.isActive());
                    Button endButton = Button.secondary("end_schedule:" + updatedSchedule.getId(), "Encerrar Escala")
                            .disabled(!updatedSchedule.isActive());

                    return scheduleLogManager.updateScheduleLogMessage(updatedSchedule, "Usuário " + username + " desembarcou")
                            .then(scheduleMessageManager.updateScheduleMessage(String.valueOf(updatedSchedule.getId()), crewNicknames))
                            .then(Mono.justOrEmpty(event.getMessage())
                                    .flatMap(message -> message.edit()
                                            .withEmbeds(embed)
                                            .withComponents(ActionRow.of(boardButton, leaveButton, endButton)))
                                    .switchIfEmpty(Mono.empty())
                                    .then());
                })
                .onErrorResume(e -> {
                    log.error("Erro ao processar botão leave_schedule para escala {}: {}", scheduleId, e.getMessage(), e);
                    String errorMessage = e instanceof IllegalArgumentException || e instanceof IllegalStateException
                            ? e.getMessage()
                            : "Erro inesperado ao desembarcar. Tente novamente ou contate o suporte.";
                    return event.createFollowup("❌ " + errorMessage).withEphemeral(true).then();
                });
    }

    public Mono<Void> handleEndSchedule(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("end_schedule:")) {
            log.error("CustomId inválido para encerrar escala: {}. Esperado prefixo 'end_schedule:'", customId);
            return event.createFollowup("❌ Erro: ID da escala inválido").withEphemeral(true).then();
        }

        Long scheduleId;
        try {
            scheduleId = Long.parseLong(customId.substring("end_schedule:".length()));
            if (scheduleId <= 0) {
                throw new NumberFormatException("ID inválido");
            }
        } catch (NumberFormatException e) {
            log.error("Erro ao parsear scheduleId de customId '{}': {}", customId, e.getMessage(), e);
            return event.createFollowup("❌ Erro: ID da escala inválido ou malformado").withEphemeral(true).then();
        }

        String userId = event.getInteraction().getUser().getId().asString();
        String username = event.getInteraction().getUser().getUsername();

        return event.deferReply(InteractionCallbackSpec.builder().ephemeral(true).build())
                .then(event.deleteReply())
                .then(Mono.justOrEmpty(event.getInteraction().getMember())
                        .flatMap(member -> {
                            String nickname = member.getNickname().orElse(username);
                            log.info("Usuário {} ({}) encerrando escala {} com apelido: {}", username, userId, scheduleId, nickname);

                            return Mono.fromCallable(() -> {
                                Schedule updatedSchedule = scheduleManager.closeScheduleAndInitialize(scheduleId, userId, nickname);
                                List<String> crewNicknames = getCrewNicknames(updatedSchedule);
                                log.info("Crew nicknames para escala {}: {}", scheduleId, crewNicknames);
                                return new ScheduleActionResult(updatedSchedule, crewNicknames);
                            });
                        })
                        .switchIfEmpty(Mono.fromCallable(() -> {
                            log.info("Usando username padrão para usuário {} ({}) na escala {}", username, userId, scheduleId);
                            Schedule updatedSchedule = scheduleManager.closeScheduleAndInitialize(scheduleId, userId, username);
                            List<String> crewNicknames = getCrewNicknames(updatedSchedule);
                            log.info("Crew nicknames para escala {}: {}", scheduleId, crewNicknames);
                            return new ScheduleActionResult(updatedSchedule, crewNicknames);
                        })))
                .flatMap(result -> {
                    Schedule updatedSchedule = result.schedule;
                    List<String> crewNicknames = result.crewNicknames != null ? result.crewNicknames : Collections.emptyList();

                    return scheduleMessageManager.removeScheduleMessage(String.valueOf(updatedSchedule.getId()))
                            .then(scheduleLogManager.createFinalScheduleLogMessage(
                                    updatedSchedule.getId(),
                                    updatedSchedule.getTitle(),
                                    updatedSchedule.getAircraftType(),
                                    updatedSchedule.getMissionType(),
                                    updatedSchedule.getActionSubType(),
                                    updatedSchedule.getActionOption(),
                                    updatedSchedule.getStartTime(),
                                    updatedSchedule.getEndTime(),
                                    updatedSchedule.getCreatedByUsername(),
                                    username,
                                    crewNicknames))
                            .then(scheduleMessageManager.updateSystemMessage())
                            .then(event.createFollowup("Escala encerrada com sucesso!").withEphemeral(true));
                })
                .onErrorResume(e -> {
                    log.error("Erro ao processar botão end_schedule para escala {}: {}", scheduleId, e.getMessage(), e);
                    String errorMessage = e instanceof IllegalArgumentException || e instanceof IllegalStateException
                            ? e.getMessage()
                            : "Erro inesperado ao encerrar escala. Tente novamente ou contate o suporte.";
                    return event.createFollowup("❌ " + errorMessage).withEphemeral(true);
                }).then();
    }

    private List<String> getCrewNicknames(Schedule schedule) {
        List<String> crewNicknames = new ArrayList<>();
        List<User> initializedCrew = schedule.getInitializedCrewMembers();
        if (initializedCrew != null) {
            for (User user : initializedCrew) {
                if (user != null && user.getNickname() != null) {
                    crewNicknames.add(user.getNickname());
                }
            }
        } else {
            log.warn("getInitializedCrewMembers retornou nulo para escala {}", schedule.getId());
        }
        return crewNicknames;
    }

    private record ScheduleActionResult(Schedule schedule, List<String> crewNicknames) {
    }
}