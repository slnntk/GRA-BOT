package com.gra.paradise.botattendance.discord;

import com.gra.paradise.botattendance.discord.buttons.ButtonDispatcher;
import com.gra.paradise.botattendance.discord.commands.Command;
import com.gra.paradise.botattendance.service.PerformanceMetricsService;
import com.gra.paradise.botattendance.service.StandbyService;
import io.micrometer.core.instrument.Timer;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordEventHandler {

    private final ApplicationContext applicationContext;
    private final GatewayDiscordClient gatewayDiscordClient;
    private final ButtonDispatcher buttonDispatcher;
    private final RestClient restClient;
    private final DiagnosticHandler diagnosticHandler;
    private final PerformanceMetricsService performanceMetrics;
    private final StandbyService standbyService;

    // Cache para otimizar lookup de comandos - evita busca linear repetida
    private final Map<String, Command> commandCache = new ConcurrentHashMap<>();

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        // Inicializar métricas de performance
        performanceMetrics.initializeMetrics();
        
        // Inicializar cache de comandos uma única vez para melhor performance
        initializeCommandCache();
        
        // Registrar comandos
        registerCommands();

        // Configurar handlers para eventos
        gatewayDiscordClient.on(ChatInputInteractionEvent.class, this::handleSlashCommand)
                .subscribe();

        gatewayDiscordClient.on(ButtonInteractionEvent.class)
                .doOnNext(e -> diagnosticHandler.logInteraction(e))
                .flatMap(buttonDispatcher::handleButtonEvent)
                .onErrorResume(e -> {
                    log.error("Erro ao processar evento de botão", e);
                    return Mono.empty();
                })
                .subscribe();

        gatewayDiscordClient.on(SelectMenuInteractionEvent.class)
                .doOnNext(e -> diagnosticHandler.logInteraction(e))
                .flatMap(buttonDispatcher::handleSelectMenuEvent)
                .onErrorResume(e -> {
                    log.error("Erro ao processar evento de menu de seleção", e);
                    return Mono.empty();
                })
                .subscribe();

        // Adicionar handler para ModalSubmitInteractionEvent
        gatewayDiscordClient.on(ModalSubmitInteractionEvent.class)
                .doOnNext(e -> diagnosticHandler.logInteraction(e))
                .flatMap(buttonDispatcher::handleModalSubmitEvent)
                .onErrorResume(e -> {
                    log.error("Erro ao processar evento de modal", e);
                    return Mono.empty();
                })
                .subscribe();

        log.info("Bot Discord iniciado com sucesso!");
    }

    // Inicializa cache de comandos para otimizar lookup - evita buscar no contexto Spring repetidamente
    private void initializeCommandCache() {
        Map<String, Command> commands = applicationContext.getBeansOfType(Command.class);
        commands.values().forEach(command -> 
            commandCache.put(command.getName(), command));
        log.info("Cache de comandos inicializado com {} comandos", commandCache.size());
    }
    private void registerCommands() {
        // Usar flatMap para evitar blocking e melhor tratamento reativo
        gatewayDiscordClient.getRestClient().getApplicationId()
                .flatMap(applicationId -> {
                    // Usar List.of para melhor performance de memória que ArrayList
                    List<ApplicationCommandRequest> commands = List.of(
                            // Comando para criar escala
                            ApplicationCommandRequest.builder()
                                    .name("criar-escala")
                                    .description("Cria uma nova escala de voo")
                                    .addOption(ApplicationCommandOptionData.builder()
                                            .name("titulo")
                                            .description("Título da escala")
                                            .type(3)
                                            .required(true)
                                            .build())
                                    .addOption(ApplicationCommandOptionData.builder()
                                            .name("aeronave")
                                            .description("Tipo de aeronave")
                                            .type(3)
                                            .required(true)
                                            .addAllChoices(createAircraftChoices())
                                            .build())
                                    .addOption(ApplicationCommandOptionData.builder()
                                            .name("missao")
                                            .description("Tipo de missão")
                                            .type(3)
                                            .required(true)
                                            .addAllChoices(createMissionTypeChoices())
                                            .build())
                                    .build(),

                            // Comando para listar escalas ativas
                            ApplicationCommandRequest.builder()
                                    .name("escalas-ativas")
                                    .description("Lista todas as escalas de voo ativas")
                                    .build(),

                            // Comando para configurar o sistema de escalas fixo
                            ApplicationCommandRequest.builder()
                                    .name("setup-escala")
                                    .description("Configura o sistema de escalas de voo no canal atual")
                                    .build(),

                                // Comando para configurar canal de logs
                                ApplicationCommandRequest.builder()
                                        .name("setup-log-channel")
                                        .description("Configura o canal atual como um canal de logs de escala")
                                        .addOption(ApplicationCommandOptionData.builder()
                                                .name("tipo")
                                                .description("Tipo de missão para este canal de logs")
                                                .type(3)
                                                .required(false)
                                                .addAllChoices(createLogChannelChoices())
                                                .build())
                                        .build(),

                                // Comando para testar stand-by
                                ApplicationCommandRequest.builder()
                                        .name("standby-status")
                                        .description("Mostra o status do sistema de stand-by do bot")
                                        .build()
                    );

                    // Registra comandos globalmente
                    return restClient.getApplicationService()
                            .bulkOverwriteGlobalApplicationCommand(applicationId, commands)
                            .doOnNext(cmd -> log.info("Comando registrado: {}", cmd.name()))
                            .then();
                })
                .subscribe();
    }

    // Cache estático para choices - evita recriar objetos a cada chamada
    private static final List<ApplicationCommandOptionChoiceData> AIRCRAFT_CHOICES = List.of(
            ApplicationCommandOptionChoiceData.builder()
                    .name("EC135")
                    .value("EC135")
                    .build(),
            ApplicationCommandOptionChoiceData.builder()
                    .name("Maverick")
                    .value("MAVERICK")
                    .build(),
            ApplicationCommandOptionChoiceData.builder()
                    .name("Valkyre")
                    .value("VALKYRE")
                    .build()
    );

    private static final List<ApplicationCommandOptionChoiceData> MISSION_TYPE_CHOICES = List.of(
            ApplicationCommandOptionChoiceData.builder()
                    .name("Patrulhamento")
                    .value("PATROL")
                    .build(),
            ApplicationCommandOptionChoiceData.builder()
                    .name("Ação")
                    .value("ACTION")
                    .build()
    );

    private static final List<ApplicationCommandOptionChoiceData> LOG_CHANNEL_CHOICES = List.of(
            ApplicationCommandOptionChoiceData.builder()
                    .name("Patrulhamento")
                    .value("PATROL")
                    .build(),
            ApplicationCommandOptionChoiceData.builder()
                    .name("Ação")
                    .value("ACTION")
                    .build(),
            ApplicationCommandOptionChoiceData.builder()
                    .name("Outros")
                    .value("OUTROS")
                    .build()
    );

    private List<ApplicationCommandOptionChoiceData> createAircraftChoices() {
        return AIRCRAFT_CHOICES; // Retorna cache estático para melhor performance
    }

    private List<ApplicationCommandOptionChoiceData> createMissionTypeChoices() {
        return MISSION_TYPE_CHOICES; // Retorna cache estático para melhor performance
    }

    private List<ApplicationCommandOptionChoiceData> createLogChannelChoices() {
        return LOG_CHANNEL_CHOICES; // Retorna cache estático para melhor performance
    }

    private Mono<Void> handleSlashCommand(ChatInputInteractionEvent event) {
        String commandName = event.getCommandName();
        log.debug("Comando recebido: {}", commandName);

        // Registrar atividade para stand-by
        standbyService.recordActivity();
        
        // Registrar evento Discord
        performanceMetrics.recordDiscordEvent();
        
        // Medir tempo de resposta
        Timer.Sample sample = performanceMetrics.startDiscordTimer();

        // Usar cache O(1) ao invés de busca linear O(n) para melhor performance
        Command command = commandCache.get(commandName);
        if (command != null) {
            return command.handle(event)
                    .doOnSuccess(result -> performanceMetrics.recordDiscordResponseTime(sample))
                    .onErrorResume(e -> {
                        performanceMetrics.recordError();
                        log.error("Erro ao executar comando {}: {}", commandName, e.getMessage(), e);
                        return event.createFollowup("Ocorreu um erro ao executar o comando: " + e.getMessage())
                                .withEphemeral(true)
                                .then();
                    });
        }

        log.warn("Comando não encontrado: {}", commandName);
        return event.reply()
                .withContent("Comando não implementado: " + commandName)
                .withEphemeral(true);
    }

    @EventListener
    public void verifyCommands(ApplicationReadyEvent event) {
        log.info("Verificando comandos disponíveis:");
        // Usar cache ao invés de buscar novamente no contexto Spring
        commandCache.forEach((commandName, command) -> {
            log.info("Comando disponível: {}", commandName);
        });
    }
}