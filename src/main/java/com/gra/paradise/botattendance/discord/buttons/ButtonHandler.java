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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ButtonHandler {

    private final ScheduleManager scheduleManager;
    private final ScheduleLogManager scheduleLogManager;
    private final EmbedFactory embedFactory;
    private final ScheduleMessageManager scheduleMessageManager;

    public Mono<Void> handleButtonClick(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        log.debug("Button clicked: {}", customId);

        if (customId.startsWith("board_schedule:")) {
            return handleJoin(event, customId);
        } else if (customId.startsWith("leave_schedule:")) {
            return handleLeave(event, customId);
        } else if (customId.startsWith("end_schedule:")) {
            return handleClose(event, customId);
        }

        return Mono.empty();
    }

    private Mono<Void> handleJoin(ButtonInteractionEvent event, String customId) {
        Long scheduleId = Long.parseLong(customId.substring("board_schedule:".length()));
        discord4j.core.object.entity.User discordUser = event.getInteraction().getUser();
        String userId = discordUser.getId().asString();
        String username = discordUser.getUsername();

        return event.deferReply(InteractionCallbackSpec.builder()
                        .ephemeral(true)
                        .build())
                .then(Mono.justOrEmpty(event.getInteraction().getMember())
                        .flatMap(member -> {
                            String nickname = member.getNickname().orElse(username);
                            log.info("Usuário embarcando: {} com apelido: {}", username, nickname);

                            return Mono.fromCallable(() -> {
                                Schedule updatedSchedule = scheduleManager.addCrewMemberAndInitialize(scheduleId, userId, username, nickname);
                                List<String> crewNicknames = new ArrayList<>();
                                for (User user : updatedSchedule.getInitializedCrewMembers()) {
                                    crewNicknames.add(user.getNickname());
                                }
                                scheduleLogManager.createScheduleLog(updatedSchedule, "JOIN", userId, nickname, nickname + " embarcou na escala");
                                return new ScheduleActionResult(updatedSchedule, crewNicknames);
                            });
                        })
                        .switchIfEmpty(Mono.fromCallable(() -> {
                            log.info("Usando username padrão para usuário: {}", username);
                            Schedule updatedSchedule = scheduleManager.addCrewMemberAndInitialize(scheduleId, userId, username, username);
                            List<String> crewNicknames = new ArrayList<>();
                            for (User user : updatedSchedule.getInitializedCrewMembers()) {
                                crewNicknames.add(user.getNickname());
                            }
                            scheduleLogManager.createScheduleLog(updatedSchedule, "JOIN", userId, username, username + " embarcou na escala");
                            return new ScheduleActionResult(updatedSchedule, crewNicknames);
                        })))
                .flatMap(result -> {
                    Schedule updatedSchedule = result.schedule;
                    List<String> crewNicknames = new ArrayList<>();
                    for (User user : updatedSchedule.getInitializedCrewMembers()) {
                        crewNicknames.add(user.getNickname());
                    }
                    EmbedCreateSpec embed = embedFactory.createSchedulePublicEmbed(updatedSchedule, crewNicknames);

                    Button joinButton = Button.success("board_schedule:" + updatedSchedule.getId(), "Embarcar")
                            .disabled(!updatedSchedule.isActive());
                    Button leaveButton = Button.danger("leave_schedule:" + updatedSchedule.getId(), "Desembarcar")
                            .disabled(!updatedSchedule.isActive());
                    Button closeButton = Button.secondary("end_schedule:" + updatedSchedule.getId(), "Encerrar Escala")
                            .disabled(!updatedSchedule.isActive());

                    return event.createFollowup("Você embarcou na aeronave com sucesso!")
                            .withEphemeral(true)
                            .then(scheduleMessageManager.updateScheduleMessage(String.valueOf(updatedSchedule.getId()), result.crewNicknames))
                            .then(scheduleLogManager.updateScheduleLogMessage(updatedSchedule, result.crewNicknames.toString()))
                            .then(Mono.justOrEmpty(event.getMessage())
                                    .flatMap(message -> message.edit()
                                            .withEmbeds(embed)
                                            .withComponents(ActionRow.of(joinButton, leaveButton, closeButton)))
                                    .then());
                })
                .onErrorResume(e -> {
                    log.error("Erro ao processar botão board_schedule", e);
                    return event.createFollowup("Erro: " + e.getMessage()).withEphemeral(true).then();
                });
    }

    private Mono<Void> handleLeave(ButtonInteractionEvent event, String customId) {
        Long scheduleId = Long.parseLong(customId.substring("leave_schedule:".length()));
        discord4j.core.object.entity.User discordUser = event.getInteraction().getUser();
        String userId = discordUser.getId().asString();
        String username = discordUser.getUsername();

        return event.deferReply(InteractionCallbackSpec.builder()
                        .ephemeral(true)
                        .build())
                .then(Mono.justOrEmpty(event.getInteraction().getMember())
                        .flatMap(member -> {
                            String nickname = member.getNickname().orElse(username);
                            log.info("Usuário desembarcando: {} com apelido: {}", username, nickname);

                            return Mono.fromCallable(() -> {
                                Schedule updatedSchedule = scheduleManager.removeCrewMemberAndInitialize(scheduleId, userId, nickname);
                                List<String> crewNicknames = new ArrayList<>();
                                for (User user : updatedSchedule.getInitializedCrewMembers()) {
                                    crewNicknames.add(user.getNickname());
                                }
                                scheduleLogManager.createScheduleLog(updatedSchedule, "LEAVE", userId, nickname, nickname + " desembarcou da escala");
                                return new ScheduleActionResult(updatedSchedule, crewNicknames);
                            });
                        })
                        .switchIfEmpty(Mono.fromCallable(() -> {
                            log.info("Usando username padrão para usuário desembarcando: {}", username);
                            Schedule updatedSchedule = scheduleManager.removeCrewMemberAndInitialize(scheduleId, userId, username);
                            List<String> crewNicknames = new ArrayList<>();
                            for (User user : updatedSchedule.getInitializedCrewMembers()) {
                                crewNicknames.add(user.getNickname());
                            }
                            scheduleLogManager.createScheduleLog(updatedSchedule, "LEAVE", userId, username, username + " desembarcou da escala");
                            return new ScheduleActionResult(updatedSchedule, crewNicknames);
                        })))
                .flatMap(result -> {
                    Schedule updatedSchedule = result.schedule;
                    List<String> crewNicknames = new ArrayList<>();
                    for (User user : updatedSchedule.getInitializedCrewMembers()) {
                        crewNicknames.add(user.getNickname());
                    }
                    EmbedCreateSpec embed = embedFactory.createSchedulePublicEmbed(updatedSchedule, crewNicknames);

                    Button joinButton = Button.success("board_schedule:" + updatedSchedule.getId(), "Embarcar")
                            .disabled(!updatedSchedule.isActive());
                    Button leaveButton = Button.danger("leave_schedule:" + updatedSchedule.getId(), "Desembarcar")
                            .disabled(!updatedSchedule.isActive());
                    Button closeButton = Button.secondary("end_schedule:" + updatedSchedule.getId(), "Encerrar Escala")
                            .disabled(!updatedSchedule.isActive());

                    return event.createFollowup("Você desembarcou da aeronave com sucesso!")
                            .withEphemeral(true)
                            .then(scheduleMessageManager.updateScheduleMessage(String.valueOf(updatedSchedule.getId()), result.crewNicknames))
                            .then(scheduleLogManager.updateScheduleLogMessage(updatedSchedule, result.crewNicknames.toString()))
                            .then(Mono.justOrEmpty(event.getMessage())
                                    .flatMap(message -> message.edit()
                                            .withEmbeds(embed)
                                            .withComponents(ActionRow.of(joinButton, leaveButton, closeButton)))
                                    .then());
                })
                .onErrorResume(e -> {
                    log.error("Erro ao processar botão leave_schedule", e);
                    return event.createFollowup("Erro: " + e.getMessage()).withEphemeral(true).then();
                });
    }

    private Mono<Void> handleClose(ButtonInteractionEvent event, String customId) {
        Long scheduleId = Long.parseLong(customId.substring("end_schedule:".length()));
        discord4j.core.object.entity.User discordUser = event.getInteraction().getUser();
        String userId = discordUser.getId().asString();
        String username = discordUser.getUsername();

        return event.deferReply(InteractionCallbackSpec.builder()
                        .ephemeral(true)
                        .build())
                .then(Mono.justOrEmpty(event.getInteraction().getMember())
                        .flatMap(member -> {
                            String nickname = member.getNickname().orElse(username);
                            log.info("Usuário encerrando escala: {} com apelido: {}", username, nickname);

                            return Mono.fromCallable(() -> {
                                Schedule updatedSchedule = scheduleManager.closeScheduleAndInitialize(scheduleId, userId, nickname);
                                List<String> crewNicknames = new ArrayList<>();
                                for (User user : updatedSchedule.getInitializedCrewMembers()) {
                                    crewNicknames.add(user.getNickname());
                                }
                                scheduleLogManager.createScheduleLog(updatedSchedule, "CLOSE", userId, nickname, nickname + " encerrou a escala");
                                return new ScheduleActionResult(updatedSchedule, crewNicknames);
                            });
                        })
                        .switchIfEmpty(Mono.fromCallable(() -> {
                            log.info("Usando username padrão para usuário: {}", username);
                            Schedule updatedSchedule = scheduleManager.closeScheduleAndInitialize(scheduleId, userId, username);
                            List<String> crewNicknames = new ArrayList<>();
                            for (User user : updatedSchedule.getInitializedCrewMembers()) {
                                crewNicknames.add(user.getNickname());
                            }
                            scheduleLogManager.createScheduleLog(updatedSchedule, "CLOSE", userId, username, username + " encerrou a escala");
                            return new ScheduleActionResult(updatedSchedule, crewNicknames);
                        })))
                .flatMap(result ->
                        event.createFollowup("Escala encerrada com sucesso!")
                                .withEphemeral(true)
                                .then(scheduleMessageManager.removeScheduleMessage(String.valueOf(result.schedule.getId())))
                                .then(scheduleLogManager.createFinalScheduleLogMessage(
                                        result.schedule.getId(),
                                        result.schedule.getTitle(),
                                        result.schedule.getAircraftType(),
                                        result.schedule.getMissionType(),
                                        result.schedule.getActionSubType(),
                                        result.schedule.getActionOption(),
                                        result.schedule.getStartTime(),
                                        result.schedule.getEndTime(),
                                        result.schedule.getCreatedByUsername(),
                                        username,
                                        result.crewNicknames))
                                .then(Mono.justOrEmpty(event.getMessage())
                                        .flatMap(message -> {
                                            log.info("Tentando deletar mensagem com ID: {}", message.getId().asString());
                                            return message.delete().then(Mono.empty());
                                        })
                                        .switchIfEmpty(Mono.defer(() -> {
                                            log.warn("Mensagem não encontrada para deleção no evento.");
                                            return Mono.empty();
                                        }))))
                .onErrorResume(e -> {
                    log.error("Erro ao processar botão end_schedule", e);
                    return event.createFollowup("Erro: " + e.getMessage()).withEphemeral(true).then();
                }).then();
    }

    private static class ScheduleActionResult {
        Schedule schedule;
        List<String> crewNicknames;

        ScheduleActionResult(Schedule schedule, List<String> crewNicknames) {
            this.schedule = schedule;
            this.crewNicknames = crewNicknames;
        }
    }
}