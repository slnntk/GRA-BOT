package com.gra.paradise.botattendance.utils;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Utilitários para Discord
 * Funções otimizadas para interação com Discord API
 */
@Slf4j
public class DiscordUtils {

    /**
     * Obtém nome de exibição otimizado
     */
    public static String getDisplayName(User user, Optional<Member> member) {
        return member.map(m -> m.getDisplayName())
                .orElse(user.getUsername());
    }

    /**
     * Obtém avatar URL otimizado
     */
    public static String getAvatarUrl(User user, int size) {
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            return avatarUrl + "?size=" + size;
        }
        return user.getDefaultAvatarUrl();
    }

    /**
     * Valida ID do Discord
     */
    public static boolean isValidDiscordId(String id) {
        return id != null && id.matches("\\d{17,19}");
    }

    /**
     * Formata timestamp do Discord
     */
    public static String formatDiscordTimestamp(java.time.Instant instant, String style) {
        return "<t:" + instant.getEpochSecond() + ":" + style + ">";
    }

    /**
     * Formata timestamp relativo
     */
    public static String formatRelativeTimestamp(java.time.Instant instant) {
        return formatDiscordTimestamp(instant, "R");
    }

    /**
     * Formata timestamp de data
     */
    public static String formatDateTimestamp(java.time.Instant instant) {
        return formatDiscordTimestamp(instant, "F");
    }

    /**
     * Cria menção de usuário
     */
    public static String createUserMention(String userId) {
        return "<@" + userId + ">";
    }

    /**
     * Cria menção de canal
     */
    public static String createChannelMention(String channelId) {
        return "<#" + channelId + ">";
    }

    /**
     * Cria menção de role
     */
    public static String createRoleMention(String roleId) {
        return "<@&" + roleId + ">";
    }

    /**
     * Extrai ID de menção
     */
    public static Optional<String> extractIdFromMention(String mention) {
        if (mention == null || !mention.startsWith("<") || !mention.endsWith(">")) {
            return Optional.empty();
        }
        
        String content = mention.substring(1, mention.length() - 1);
        if (content.startsWith("@") && !content.startsWith("@&")) {
            return Optional.of(content.substring(1));
        }
        return Optional.empty();
    }

    /**
     * Valida permissões de usuário
     */
    public static CompletableFuture<Boolean> hasPermission(Member member, String permission) {
        return member.getBasePermissions()
                .map(permissions -> permissions.contains(discord4j.rest.util.Permission.valueOf(permission)))
                .toFuture();
    }

    /**
     * Formata duração para Discord
     */
    public static String formatDuration(java.time.Duration duration) {
        long totalSeconds = duration.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Cria embed otimizado
     */
    public static String createOptimizedEmbedTitle(String title, String description) {
        return "**" + title + "**\n" + description;
    }

    /**
     * Valida tamanho de mensagem
     */
    public static boolean isValidMessageLength(String message) {
        return message != null && message.length() <= 2000;
    }

    /**
     * Trunca mensagem se necessário
     */
    public static String truncateMessage(String message, int maxLength) {
        if (message == null) return "";
        if (message.length() <= maxLength) return message;
        return message.substring(0, maxLength - 3) + "...";
    }
}
