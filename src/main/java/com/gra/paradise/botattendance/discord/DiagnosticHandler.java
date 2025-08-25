package com.gra.paradise.botattendance.discord;

import com.gra.paradise.botattendance.service.MemoryMonitoringService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiagnosticHandler {
    
    private final MemoryMonitoringService memoryMonitoringService;

    public DiagnosticHandler(GatewayDiscordClient client, MemoryMonitoringService memoryMonitoringService) {
        this.memoryMonitoringService = memoryMonitoringService;
        // Este handler vai registrar TODAS as interações que o bot recebe
//        client.on(InteractionCreateEvent.class)
//                .subscribe(event -> {
//                    log.info("=========== INTERAÇÃO RECEBIDA ===========");
//                    log.info("Tipo: {}", event.getClass().getSimpleName());
//                    log.info("De: {}", event.getInteraction().getUser().getUsername());
//                    log.info("Memory: {}", memoryMonitoringService.getMemoryStats());
//                    log.info("==========================================");
//                });
    }

    public void logInteraction(DeferrableInteractionEvent event) {
//        log.debug("Interação recebida: {} de usuário: {} - {}",
//                event.getInteraction().getType().name(),
//                event.getInteraction().getUser().getUsername(),
//                memoryMonitoringService.getMemoryStats());
    }
}