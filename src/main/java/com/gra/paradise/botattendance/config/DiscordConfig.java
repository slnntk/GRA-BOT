package com.gra.paradise.botattendance.config;

import com.gra.paradise.botattendance.model.AircraftType;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.rest.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do cliente Discord para o bot de attendance.
 *
 * @author Tiago Holanda
 */
@Configuration
public class DiscordConfig {

    @Value("${discord.token}")
    private String token;
//    public static final String FOOTER_IMAGE_URL = "https://raw.githubusercontent.com/slnntk/GRA-BOT/main/src/main/resources/images/image.png";
    public static final Map<AircraftType, String> AIRCRAFT_IMAGE_URLS = new HashMap<>();
    public static final String GRA_IMAGE_URL = "https://raw.githubusercontent.com/slnntk/qru-report/refs/heads/main/src/assets/EMBLEMA%20GRA.png";
    public static final String FOOTER_GRA_BLUE_URL = "https://raw.githubusercontent.com/slnntk/BOT-ATTENDANCE-GRA/refs/heads/master/image.png";
    public static final String FOOTER_GRA_BLACK_URL = "https://raw.githubusercontent.com/slnntk/BOT-ATTENDANCE-GRA/refs/heads/master/gra_gutem.png";


    static {

        AIRCRAFT_IMAGE_URLS.put(AircraftType.VALKYRE, "https://raw.githubusercontent.com/slnntk/GRA-BOT/main/src/main/resources/images/image.png");
        AIRCRAFT_IMAGE_URLS.put(AircraftType.EC135, "https://raw.githubusercontent.com/slnntk/GRA-BOT/main/src/main/resources/images/image.png");
        AIRCRAFT_IMAGE_URLS.put(AircraftType.MAVERICK, "https://raw.githubusercontent.com/slnntk/GRA-BOT/main/src/main/resources/images/image.png");
        // Add more AircraftType mappings as needed
    }

    @Bean
    public GatewayDiscordClient gatewayDiscordClient() {
        return DiscordClientBuilder.create(token)
                .build()
                .gateway()
                .setInitialPresence(shardInfo ->
                        ClientPresence.online(ClientActivity.playing("Desenvolvido por Tiago Holanda")))
                .login()
                .block();
    }

    @Bean
    public RestClient discordRestClient(GatewayDiscordClient gatewayDiscordClient) {
        return gatewayDiscordClient.getRestClient();
    }
}