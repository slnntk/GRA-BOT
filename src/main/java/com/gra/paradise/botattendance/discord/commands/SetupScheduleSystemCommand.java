package com.gra.paradise.botattendance.discord.commands;

import com.gra.paradise.botattendance.model.GuildConfig;
import com.gra.paradise.botattendance.repository.GuildConfigRepository;
import com.gra.paradise.botattendance.service.ScheduleMessageManager;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.gra.paradise.botattendance.config.DiscordConfig.FOOTER_GRA_BLUE_URL;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "discord.enabled", havingValue = "true", matchIfMissing = true)
public class SetupScheduleSystemCommand implements Command {

    private static final ZoneId FORTALEZA_ZONE = ZoneId.of("America/Fortaleza");
    private final ScheduleMessageManager scheduleMessageManager;
    private final GuildConfigRepository guildConfigRepository;

    @Override
    public String getName() {
        return "setup-escala";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        log.info("Comando setup-escala recebido de {}", event.getInteraction().getUser().getUsername());

        String guildId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("Comando deve ser executado em um servidor"));
        String channelId = event.getInteraction().getChannelId().asString();

        // Salvar o canal no banco de dados
        return Mono.fromCallable(() -> {
            GuildConfig config = guildConfigRepository.findById(guildId)
                    .orElse(new GuildConfig());
            config.setGuildId(guildId);
            config.setSystemChannelId(channelId);
            guildConfigRepository.save(config);
            return config;
        }).flatMap(config -> {
            log.info("Canal de sistema configurado para guilda {}: canal {}", guildId, channelId);
            // Criar a mensagem do sistema
            EmbedCreateSpec embed = createSystemEmbed();
            Button createButton = Button.primary("create_schedule", "Criar Escala");

            return event.getInteraction().getChannel()
                    .flatMap(channel -> channel.createMessage()
                            .withEmbeds(embed)
                            .withComponents(ActionRow.of(createButton)))
                    .flatMap(message -> {
                        String messageId = message.getId().asString();
                        return scheduleMessageManager.registerSystemMessage(guildId, channelId, messageId)
                                .then(scheduleMessageManager.updateSystemMessage(guildId))
                                .then(event.reply()
                                        .withEphemeral(true)
                                        .withContent("✅ Sistema de escalas configurado com sucesso!"));
                    });
        }).onErrorResume(e -> {
            log.error("Erro ao configurar sistema de escalas para guilda {}: {}", guildId, e.getMessage(), e);
            return event.reply()
                    .withEphemeral(true)
                    .withContent("❌ Erro ao configurar o sistema de escalas: " + e.getMessage());
        });
    }

    private EmbedCreateSpec createSystemEmbed() {
        return EmbedCreateSpec.builder()
                .image(FOOTER_GRA_BLUE_URL)
                .title("Sistema de Escalas de Voo")
                .description("Organize suas escalas de voo com estilo! Clique no botão abaixo para começar.")
                .color(Color.CYAN)
                .addField("Instruções",
                        "1. Clique em 'Criar Escala'\n" +
                                "2. Escolha a aeronave\n" +
                                "3. Defina o tipo de missão\n" +
                                "4. Confirme sua escala", false)
                .addField("Escalas Ativas", "Nenhuma escala ativa no momento. Que tal criar uma agora?", false)
                .footer("G.R.A Bot - Escala de Voo", FOOTER_GRA_BLUE_URL)
                .timestamp(ZonedDateTime.now(FORTALEZA_ZONE).toInstant())
                .build();
    }
}