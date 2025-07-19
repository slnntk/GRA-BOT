package com.gra.paradise.botattendance.config;

import com.gra.paradise.botattendance.model.AircraftType;
import com.gra.paradise.botattendance.model.GuildConfig;
import com.gra.paradise.botattendance.model.MissionType;
import com.gra.paradise.botattendance.repository.GuildConfigRepository;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.rest.RestClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do cliente Discord para o bot de attendance.
 *
 * @author Tiago Holanda
 */
@Configuration
@RequiredArgsConstructor
public class DiscordConfig {

    private static final Logger logger = LoggerFactory.getLogger(DiscordConfig.class);
    public static final ZoneId FORTALEZA_ZONE = ZoneId.of("America/Fortaleza");
    private final GuildConfigRepository guildConfigRepository;

    @Value("${discord.token}")
    private String token;

    @Value("${gra.image.url:https://raw.githubusercontent.com/slnntk/qru-report/refs/heads/main/src/assets/EMBLEMA%20GRA.png}")
    private String graImageUrl;
    @Value("${footer.gra.blue.url:https://raw.githubusercontent.com/slnntk/BOT-ATTENDANCE-GRA/refs/heads/master/image.png}")
    private String footerGraBlueUrl;
    @Value("${heli.first.screen:https://raw.githubusercontent.com/slnntk/BOT-ATTENDANCE-GRA/refs/heads/master/gra_gutem.png}")
    private String heliFirstScreenUrl;
    @Value("${aircraft.valkyre.url:https://raw.githubusercontent.com/slnntk/GRA-BOT/main/src/main/resources/images/image.png}")
    private String valkyreImageUrl;
    @Value("${aircraft.ec135.url:https://raw.githubusercontent.com/slnntk/GRA-BOT/main/src/main/resources/images/image.png}")
    private String ec135ImageUrl;
    @Value("${aircraft.maverick.url:https://raw.githubusercontent.com/slnntk/GRA-BOT/main/src/main/resources/images/image.png}")
    private String maverickImageUrl;

    public static final Map<AircraftType, String> AIRCRAFT_IMAGE_URLS = new HashMap<>();
    public static String GRA_IMAGE_URL;
    public static String FOOTER_GRA_BLUE_URL;
    public static String FOOTER_CHOOSE_HELI_FIRST_SCREEN_URL;

    @PostConstruct
    public void init() {
        GRA_IMAGE_URL = graImageUrl;
        FOOTER_GRA_BLUE_URL = footerGraBlueUrl;
        FOOTER_CHOOSE_HELI_FIRST_SCREEN_URL = heliFirstScreenUrl;
        AIRCRAFT_IMAGE_URLS.put(AircraftType.VALKYRE, valkyreImageUrl);
        AIRCRAFT_IMAGE_URLS.put(AircraftType.EC135, ec135ImageUrl);
        AIRCRAFT_IMAGE_URLS.put(AircraftType.MAVERICK, maverickImageUrl);
        logger.debug("Initialized URLs: GRA_IMAGE_URL={}, FOOTER_GRA_BLUE_URL={}, FOOTER_CHOOSE_HELI_FIRST_SCREEN_URL={}",
                GRA_IMAGE_URL, FOOTER_GRA_BLUE_URL, FOOTER_CHOOSE_HELI_FIRST_SCREEN_URL);
        logger.debug("AIRCRAFT_IMAGE_URLS: {}", AIRCRAFT_IMAGE_URLS);
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

    public String getDefaultSystemChannelId(String guildId) {
        return guildConfigRepository.findById(guildId)
                .map(GuildConfig::getSystemChannelId)
                .orElse(null);
    }

    public String getLogChannelId(String guildId, MissionType missionType) {
        return guildConfigRepository.findById(guildId)
                .map(config -> {
                    if (missionType == MissionType.ACTION) {
                        return config.getActionLogChannelId();
                    } else if (missionType == MissionType.PATROL) {
                        return config.getPatrolLogChannelId();
                    } else if (missionType == MissionType.OUTROS) {
                        return config.getOutrosLogChannelId();
                    } else {
                        // Fallback to system channel if mission type is null or unknown
                        return config.getSystemChannelId();
                    }
                })
                .orElse(null);
    }
}