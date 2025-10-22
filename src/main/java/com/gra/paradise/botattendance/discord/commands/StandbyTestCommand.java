package com.gra.paradise.botattendance.discord.commands;

import com.gra.paradise.botattendance.service.StandbyService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Comando para testar o sistema de stand-by
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StandbyTestCommand implements Command {

    private final StandbyService standbyService;

    @Override
    public String getName() {
        return "standby-status";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.deferReply()
                .then(createStandbyStatusEmbed())
                .flatMap(embed -> event.editReply()
                        .withEmbeds(embed))
                .then();
    }

    private Mono<EmbedCreateSpec> createStandbyStatusEmbed() {
        return Mono.fromCallable(() -> {
            String status = standbyService.getStandbyStatus();
            long timeSinceActivity = standbyService.getTimeSinceLastActivity();
            long standbyDuration = standbyService.getStandbyDuration();
            boolean isStandby = standbyService.isStandby();

            String description = String.format(
                "**Status do Bot:** %s\n" +
                "**Tempo desde última atividade:** %d segundos\n" +
                "**Tempo em stand-by:** %d segundos\n" +
                "**Modo atual:** %s",
                status,
                timeSinceActivity / 1000,
                standbyDuration / 1000,
                isStandby ? "🔴 Stand-by" : "🟢 Ativo"
            );

            return EmbedCreateSpec.builder()
                    .title("🤖 Status do Sistema de Stand-by")
                    .description(description)
                    .color(isStandby ? Color.RED : Color.GREEN)
                    .addField("⏰ Configuração", "Stand-by após 3 minutos de inatividade", false)
                    .addField("🔄 Verificação", "A cada 1 minuto", false)
                    .addField("💾 Cache", "TTL de 5 minutos", false)
                    .build();
        });
    }
}
