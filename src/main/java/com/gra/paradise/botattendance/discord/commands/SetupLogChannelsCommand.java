package com.gra.paradise.botattendance.discord.commands;

import com.gra.paradise.botattendance.model.GuildConfig;
import com.gra.paradise.botattendance.model.MissionType;
import com.gra.paradise.botattendance.repository.GuildConfigRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SetupLogChannelsCommand implements Command {

    private final GuildConfigRepository guildConfigRepository;

    @Override
    public String getName() {
        return "setup-log-channel";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        log.info("Comando setup-log-channel recebido de {}", event.getInteraction().getUser().getUsername());

        String guildId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("Comando deve ser executado em um servidor"));
        String channelId = event.getInteraction().getChannelId().asString();

        // Log todos os parâmetros para debug
        event.getOptions().forEach(opt -> {
            log.info("Opção recebida: {} = {}", opt.getName(),
                    opt.getValue().map(ApplicationCommandInteractionOptionValue::asString).orElse("sem valor"));
        });

        // Extrair tipo da missão
        Optional<String> missionTypeStr = event.getOption("tipo")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);

        log.info("Parâmetro tipo recebido: {}", missionTypeStr.orElse("não especificado"));

        // Validar tipo de missão
        MissionType missionType;
        String logTypeMessage;
        try {
            missionType = missionTypeStr.map(str -> MissionType.valueOf(str.toUpperCase()))
                    .orElseThrow(() -> new IllegalArgumentException("Tipo de missão é obrigatório"));
            logTypeMessage = "missões do tipo " + missionType.getDisplayName();
            log.info("Tipo de missão configurado: {}", missionType);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de missão inválido: {}", missionTypeStr.orElse("não especificado"));
            return event.reply()
                    .withEphemeral(true)
                    .withContent("❌ Tipo de missão inválido ou não especificado. Valores permitidos: ACTION, PATROL, OUTROS");
        }

        // Configurar o canal no banco de dados
        return Mono.fromCallable(() -> {
            GuildConfig config = guildConfigRepository.findById(guildId)
                    .orElse(new GuildConfig());
            config.setGuildId(guildId);
            if (missionType == MissionType.ACTION) {
                config.setActionLogChannelId(channelId);
            } else if (missionType == MissionType.PATROL) {
                config.setPatrolLogChannelId(channelId);
            } else if (missionType == MissionType.OUTROS) {
                config.setOutrosLogChannelId(channelId);
            }
            guildConfigRepository.save(config);
            return config;
        }).flatMap(config -> {
            log.info("Canal de log configurado para guilda {}: {} para {}", guildId, channelId, logTypeMessage);
            return event.reply()
                    .withEphemeral(true)
                    .withContent("✅ Este canal foi configurado como canal de logs para " + logTypeMessage + ".");
        }).onErrorResume(e -> {
            log.error("Erro ao configurar canal de logs para guilda {}: {}", guildId, e.getMessage(), e);
            return event.reply()
                    .withEphemeral(true)
                    .withContent("❌ Erro ao configurar o canal: " + e.getMessage());
        });
    }
}