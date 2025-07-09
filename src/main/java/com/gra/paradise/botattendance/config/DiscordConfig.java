package com.gra.paradise.botattendance.config;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.rest.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do cliente Discord para o bot de attendance.
 *
 * @author Tiago Holanda
 */
@Configuration
public class DiscordConfig {

    @Value("${discord.token}")
    private String token;

    @Bean
    public GatewayDiscordClient gatewayDiscordClient() {
        return DiscordClientBuilder.create(token)
                .build()
                .gateway()
                .setInitialPresence(shardInfo ->
                        ClientPresence.online(ClientActivity.playing("Gerenciando escalas de voo | Desenvolvido por Tiago Holanda")))
                .login()
                .block();
    }

    @Bean
    public RestClient discordRestClient(GatewayDiscordClient gatewayDiscordClient) {
        return gatewayDiscordClient.getRestClient();
    }
}