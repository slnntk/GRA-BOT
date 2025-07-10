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
public class ScheduleActionHandler {

    private final ScheduleManager scheduleManager;
    private final EmbedFactory embedFactory;
    private final ScheduleMessageManager scheduleMessageManager;
    private final ScheduleLogManager scheduleLogManager;

    public Mono<Void> handleBoardSchedule(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        Long scheduleId = Long.parseLong(customId.substring("board_schedule:".length()));
        String userId = event.getInteraction().getUser().getId().asString();
        String username = event.getInteraction().getUser().getUsername();

        return event.deferReply(InteractionCallbackSpec.builder().ephemeral(true).build())
                .then(event.deleteReply())
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
                                log.info("Crew nicknames for schedule {}: {}", scheduleId, crewNicknames);
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
                            log.info("Crew nicknames for schedule {}: {}", scheduleId, crewNicknames);
                            return new ScheduleActionResult(updatedSchedule, crewNicknames);
                        })))
                .flatMap(result -> {
                    Schedule updatedSchedule = result.schedule;
                    List<String> crewNicknames = result.crewNicknames;
                    EmbedCreateSpec embed = embedFactory.createSchedulePublicEmbed(updatedSchedule, crewNicknames);

                    Button boardButton = Button.success("board_schedule:" + updatedSchedule.getId(), "Embarcar")
                            .disabled(!updatedSchedule.isActive());
                    Button leaveButton = Button.danger("leave_schedule:" + updatedSchedule.getId(), "Desembarcar")
                            .disabled(!updatedSchedule.isActive());
                    Button endButton = Button.secondary("end_schedule:" + updatedSchedule.getId(), "Encerrar Escala")
                            .disabled(!updatedSchedule.isActive());
                    return scheduleLogManager.updateScheduleLogMessage(updatedSchedule, "JOINED")
                            .then()
                            .then(scheduleLogManager.updateScheduleLogMessage(updatedSchedule, "Usuário " + username + " embarcou"))
                            .then(scheduleMessageManager.updateScheduleMessage(String.valueOf(updatedSchedule.getId()), crewNicknames))
                            .then(Mono.justOrEmpty(event.getMessage())
                                    .flatMap(message -> message.edit()
                                            .withEmbeds(embed)
                                            .withComponents(ActionRow.of(boardButton, leaveButton, endButton)))
                                    .then());
                })
                .onErrorResume(e -> {
                    log.error("Erro ao processar botão board_schedule para escala {}: {}", scheduleId, e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao embarcar: " + e.getMessage())
                            .withEphemeral(true)
                            .then();
                });
    }

    public Mono<Void> handleLeaveSchedule(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        Long scheduleId = Long.parseLong(customId.substring("leave_schedule:".length()));
        String userId = event.getInteraction().getUser().getId().asString();
        String username = event.getInteraction().getUser().getUsername();

        return event.deferReply(InteractionCallbackSpec.builder().ephemeral(true).build())
                .then(event.deleteReply())
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
                                log.info("Crew nicknames for schedule {}: {}", scheduleId, crewNicknames);
                                return new ScheduleActionResult(updatedSchedule, crewNicknames);
                            });
                        })
                        .switchIfEmpty(Mono.fromCallable(() -> {
                            log.info("Usando username padrão para usuário: {}", username);
                            Schedule updatedSchedule = scheduleManager.removeCrewMemberAndInitialize(scheduleId, userId, username);
                            List<String> crewNicknames = new ArrayList<>();
                            for (User user : updatedSchedule.getInitializedCrewMembers()) {
                                crewNicknames.add(user.getNickname());
                            }
                            log.info("Crew nicknames for schedule {}: {}", scheduleId, crewNicknames);
                            return new ScheduleActionResult(updatedSchedule, crewNicknames);
                        })))
                .flatMap(result -> {
                    Schedule updatedSchedule = result.schedule;
                    List<String> crewNicknames = result.crewNicknames;
                    EmbedCreateSpec embed = embedFactory.createSchedulePublicEmbed(updatedSchedule, crewNicknames);

                    Button boardButton = Button.success("board_schedule:" + updatedSchedule.getId(), "Embarcar")
                            .disabled(!updatedSchedule.isActive());
                    Button leaveButton = Button.danger("leave_schedule:" + updatedSchedule.getId(), "Desembarcar")
                            .disabled(!updatedSchedule.isActive());
                    Button endButton = Button.secondary("end_schedule:" + updatedSchedule.getId(), "Encerrar Escala")
                            .disabled(!updatedSchedule.isActive());

                    return scheduleLogManager.updateScheduleLogMessage(updatedSchedule, "LEFT")
                            .then()
                            .then(scheduleLogManager.updateScheduleLogMessage(updatedSchedule, "Usuário " + username + " desembarcou"))
                            .then(scheduleMessageManager.updateScheduleMessage(String.valueOf(updatedSchedule.getId()), crewNicknames))
                            .then(Mono.justOrEmpty(event.getMessage())
                                    .flatMap(message -> message.edit()
                                            .withEmbeds(embed)
                                            .withComponents(ActionRow.of(boardButton, leaveButton, endButton)))
                                    .then());
                })
                .onErrorResume(e -> {
                    log.error("Erro ao processar botão leave_schedule para escala {}: {}", scheduleId, e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao desembarcar: " + e.getMessage())
                            .withEphemeral(true)
                            .then();
                });
    }

    public Mono<Void> handleEndSchedule(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        Long scheduleId = Long.parseLong(customId.substring("end_schedule:".length()));
        String userId = event.getInteraction().getUser().getId().asString();
        String username = event.getInteraction().getUser().getUsername();

        return event.deferReply(InteractionCallbackSpec.builder().ephemeral(true).build())
                .then(event.deleteReply())
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
                                log.info("Crew nicknames for schedule {}: {}", scheduleId, crewNicknames);
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
                            log.info("Crew nicknames for schedule {}: {}", scheduleId, crewNicknames);
                            return new ScheduleActionResult(updatedSchedule, crewNicknames);
                        })))
                .flatMap(result -> {
                    Schedule updatedSchedule = result.schedule;
                    List<String> crewNicknames = result.crewNicknames;
                    EmbedCreateSpec embed = embedFactory.createSchedulePublicEmbed(updatedSchedule, crewNicknames);

                    Button boardButton = Button.success("board_schedule:" + updatedSchedule.getId(), "Embarcar")
                            .disabled(true);
                    Button leaveButton = Button.danger("leave_schedule:" + updatedSchedule.getId(), "Desembarcar")
                            .disabled(true);
                    Button endButton = Button.secondary("end_schedule:" + updatedSchedule.getId(), "Encerrar Escala")
                            .disabled(true);

                    return scheduleLogManager.updateScheduleLogMessage(updatedSchedule, "CLOSED")
                            .then()
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
                            .then(scheduleMessageManager.updateScheduleMessage(String.valueOf(updatedSchedule.getId()), crewNicknames))
                            .then(Mono.justOrEmpty(event.getMessage())
                                    .flatMap(message -> message.edit()
                                            .withEmbeds(embed)
                                            .withComponents(ActionRow.of(boardButton, leaveButton, endButton)))
                                    .then())
                            .then(scheduleMessageManager.updateSystemMessage());
                })
                .onErrorResume(e -> {
                    log.error("Erro ao processar botão end_schedule para escala {}: {}", scheduleId, e.getMessage(), e);
                    return event.createFollowup("❌ Erro ao encerrar escala: " + e.getMessage())
                            .withEphemeral(true)
                            .then();
                });
    }

    private record ScheduleActionResult(Schedule schedule, List<String> crewNicknames) {
    }
}