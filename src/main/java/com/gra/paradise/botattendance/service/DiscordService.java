package com.gra.paradise.botattendance.service;

import discord4j.common.util.Snowflake;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.PartialMemberData;
import discord4j.rest.RestClient;
import discord4j.rest.service.GuildService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscordService {
    private static final Logger logger = LoggerFactory.getLogger(DiscordService.class);
    private final RestClient restClient;

    public DiscordService(RestClient restClient) {
        this.restClient = restClient;
    }

    public boolean checkUserHasRole(String guildId, String discordId, String roleId) {
        try {
            GuildService guildService = restClient.getGuildService();
            List<Id> roleIds = guildService.getGuildMember(Snowflake.of(guildId).asLong(), Snowflake.of(discordId).asLong())
                    .map(PartialMemberData::roles)
                    .block();

            if (roleIds != null && !roleIds.isEmpty()) {
                Id roleIdToCheck = Id.of(roleId);
                return roleIds.contains(roleIdToCheck);
            }
            return false;
        } catch (Exception e) {
            logger.error("Erro ao verificar cargo do usuário {} na guilda {}: {}", discordId, guildId, e.getMessage());
            return false;
        }
    }
}