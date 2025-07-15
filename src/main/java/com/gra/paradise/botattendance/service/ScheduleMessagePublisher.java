package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.model.Schedule;
import com.gra.paradise.botattendance.model.User;
import com.gra.paradise.botattendance.repository.ScheduleRepository;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.common.util.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleMessagePublisher {

    private final GatewayDiscordClient discordClient;
    private final EmbedFactory embedFactory;
    private final ScheduleMessageManager scheduleMessageManager;
    private final ScheduleRepository scheduleRepository;

    public Mono<Void> createSchedulePublicMessage(ButtonInteractionEvent event, Schedule schedule) {
        List<String> crewNicknames = new ArrayList<>();
        for (User user : schedule.getInitializedCrewMembers()) {
            crewNicknames.add(user.getNickname());
        }
        EmbedCreateSpec embed = embedFactory.createSchedulePublicEmbed(schedule, crewNicknames);

        Button boardButton = Button.success("board_schedule:" + schedule.getId(), "Embarcar");
        Button leaveButton = Button.danger("leave_schedule:" + schedule.getId(), "Desembarcar");
        Button endButton = Button.secondary("end_schedule:" + schedule.getId(), "Encerrar Escala");

        return event.getInteraction().getChannel()
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                        .addEmbed(embed)
                        .addComponent(ActionRow.of(boardButton, leaveButton, endButton))
                        .build()))
                .flatMap(message -> {
                    String messageId = message.getId().asString();
                    String channelId = message.getChannelId().asString();
                    schedule.setMessageId(messageId);
                    schedule.setChannelId(channelId);
                    return Mono.just(scheduleRepository.save(schedule))
                            .then(scheduleMessageManager.registerScheduleMessage(String.valueOf(schedule.getId()), channelId, messageId));
                })
                .doOnSuccess(v -> log.info("Mensagem pública criada para a escala {} com messageId {} e channelId {}", schedule.getId(), schedule.getMessageId(), schedule.getChannelId()))
                .doOnError(e -> log.error("Erro ao criar mensagem pública para a escala {}: {}", schedule.getId(), e.getMessage()))
                .then();
    }
}