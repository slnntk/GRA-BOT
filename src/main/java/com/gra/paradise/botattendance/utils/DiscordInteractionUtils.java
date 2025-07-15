package com.gra.paradise.botattendance.utils;

import com.gra.paradise.botattendance.model.AircraftType;
import com.gra.paradise.botattendance.model.MissionType;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionCallbackSpec;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@UtilityClass
public class DiscordInteractionUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static Mono<Void> replyError(ButtonInteractionEvent event, String msg) {
        return event.createFollowup("\u274C " + msg).withEphemeral(true).then();
    }

    public static Mono<Void> replyError(SelectMenuInteractionEvent event, String msg) {
        return event.createFollowup("\u274C " + msg).withEphemeral(true).then();
    }

    public static String getNicknameOrUsername(Member member, User user) {
        return member.getNickname().orElse(user.getUsername());
    }

    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    public static Optional<AircraftType> parseAircraft(String raw) {
        try {
            return Optional.of(AircraftType.valueOf(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<MissionType> parseMission(String raw) {
        try {
            return Optional.of(MissionType.valueOf(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Long safeParseId(String customId, String prefix, Logger logger) {
        try {
            return Long.parseLong(customId.substring(prefix.length()));
        } catch (Exception e) {
            logger.warn("Erro ao parsear ID do customId '{}': {}", customId, e.getMessage());
            return null;
        }
    }

    public static String getGuildId(ButtonInteractionEvent event) {
        return event.getInteraction().getGuildId()
                .map(snowflake -> snowflake.asString())
                .orElseThrow(() -> new IllegalStateException("Comando deve ser usado em servidor"));
    }

    public static String getUserId(ButtonInteractionEvent event) {
        return event.getInteraction().getUser().getId().asString();
    }

    public static String getUsername(ButtonInteractionEvent event) {
        return event.getInteraction().getUser().getUsername();
    }

    public static Mono<Void> deferEphemeral(ButtonInteractionEvent event) {
        return event.deferReply(InteractionCallbackSpec.builder().ephemeral(true).build());
    }
}
