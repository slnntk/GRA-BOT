package com.gra.paradise.botattendance.discord.commands;

import com.gra.paradise.botattendance.model.PatrolContest;
import com.gra.paradise.botattendance.model.PatrolParticipant;
import com.gra.paradise.botattendance.service.PatrolContestService;
import com.gra.paradise.botattendance.service.PatrolLotteryService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatrolContestCommand implements Command {

    private final PatrolContestService patrolContestService;
    private final PatrolLotteryService patrolLotteryService;
    
    @Override
    public String getName() {
        return "patrol-contest";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String guildId = event.getInteraction().getGuildId()
                .map(discord4j.common.util.Snowflake::asString)
                .orElse("");
        
        String subCommand = event.getOptions().get(0).getName();
        
        return switch (subCommand) {
            case "create" -> handleCreateContest(event, guildId);
            case "status" -> handleContestStatus(event, guildId);
            case "check" -> handleCheckHours(event, guildId);
            case "leaderboard" -> handleLeaderboard(event, guildId);
            case "draw" -> handleDrawWinners(event, guildId);
            case "winners" -> handleShowWinners(event, guildId);
            default -> event.reply("❌ Subcomando desconhecido!").withEphemeral(true);
        };
    }
    
    private Mono<Void> handleCreateContest(ChatInputInteractionEvent event, String guildId) {
        List<ApplicationCommandInteractionOption> options = event.getOptions().get(0).getOptions();
        
        String title = getOptionValue(options, "title");
        String description = getOptionValue(options, "description");
        String startDateStr = getOptionValue(options, "start-date");
        String endDateStr = getOptionValue(options, "end-date");
        
        if (title == null || startDateStr == null || endDateStr == null) {
            return event.reply("❌ Parâmetros obrigatórios: title, start-date, end-date").withEphemeral(true);
        }
        
        try {
            LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            LocalDate endDate = LocalDate.parse(endDateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            
            String createdBy = event.getInteraction().getMember()
                    .map(member -> member.getNickname()
                            .orElse(event.getInteraction().getUser().getUsername()))
                    .orElse(event.getInteraction().getUser().getUsername());
            
            PatrolContest contest = patrolContestService.createContest(
                    guildId, title, description != null ? description : "", 
                    startDate, endDate, createdBy);
            
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .color(Color.GREEN)
                    .title("🏆 Concurso de Patrulha Criado!")
                    .description(String.format(
                            "**%s**\n\n" +
                            "%s\n\n" +
                            "📅 **Período:** %s até %s\n" +
                            "⏰ **Meta:** %d horas de patrulha\n" +
                            "📈 **Limite diário:** %.1f horas\n\n" +
                            "🕐 **Período da Tarde:** %s às %s\n" +
                            "🌙 **Período da Noite:** %s às %s\n\n" +
                            "🎁 **Prêmios da Tarde:** %d ganhadores (Diamantes/Skins)\n" +
                            "💎 **VIP Diamante:** %d ganhadores",
                            contest.getTitle(),
                            contest.getDescription(),
                            startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            contest.getRequiredHours(),
                            contest.getMaxDailyHours(),
                            contest.getAfternoonStart(),
                            contest.getAfternoonEnd(),
                            contest.getNightStart(),
                            contest.getNightEnd(),
                            contest.getAfternoonWinners(),
                            contest.getNightVipWinners()))
                    .build();
            
            return event.reply().withEmbeds(embed);
            
        } catch (DateTimeParseException e) {
            return event.reply("❌ Formato de data inválido! Use dd/MM/yyyy (ex: 15/08/2024)").withEphemeral(true);
        } catch (Exception e) {
            log.error("Error creating patrol contest", e);
            return event.reply("❌ Erro ao criar concurso: " + e.getMessage()).withEphemeral(true);
        }
    }
    
    private Mono<Void> handleContestStatus(ChatInputInteractionEvent event, String guildId) {
        Optional<PatrolContest> activeContest = patrolContestService.getActiveContest(guildId);
        
        if (activeContest.isEmpty()) {
            return event.reply("❌ Não há concurso ativo no momento.").withEphemeral(true);
        }
        
        PatrolContest contest = activeContest.get();
        List<PatrolParticipant> eligible = patrolContestService.getEligibleParticipants(contest.getId());
        
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.BLUE)
                .title("📊 Status do Concurso")
                .description(String.format(
                        "**%s**\n\n" +
                        "%s\n\n" +
                        "📅 **Período:** %s até %s\n" +
                        "👥 **Participantes elegíveis:** %d\n\n" +
                        "⏰ **Meta:** %d horas de patrulha\n" +
                        "📈 **Limite diário:** %.1f horas\n\n" +
                        "🕐 **Tarde (%s-%s):** %d ganhadores\n" +
                        "🌙 **Noite (%s-%s):** %d VIPs",
                        contest.getTitle(),
                        contest.getDescription(),
                        contest.getStartDate().atZone(java.time.ZoneId.of("America/Fortaleza"))
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        contest.getEndDate().atZone(java.time.ZoneId.of("America/Fortaleza"))
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        eligible.size(),
                        contest.getRequiredHours(),
                        contest.getMaxDailyHours(),
                        contest.getAfternoonStart(),
                        contest.getAfternoonEnd(),
                        contest.getAfternoonWinners(),
                        contest.getNightStart(),
                        contest.getNightEnd(),
                        contest.getNightVipWinners()))
                .build();
        
        return event.reply().withEmbeds(embed);
    }
    
    private Mono<Void> handleCheckHours(ChatInputInteractionEvent event, String guildId) {
        Optional<PatrolContest> activeContest = patrolContestService.getActiveContest(guildId);
        
        if (activeContest.isEmpty()) {
            return event.reply("❌ Não há concurso ativo no momento.").withEphemeral(true);
        }
        
        String userId = event.getInteraction().getUser().getId().asString();
        Optional<PatrolParticipant> participant = patrolContestService
                .getParticipant(activeContest.get().getId(), userId);
        
        if (participant.isEmpty()) {
            return event.reply("📊 Você ainda não possui horas de patrulha registradas neste concurso.")
                    .withEphemeral(true);
        }
        
        PatrolParticipant p = participant.get();
        PatrolContest contest = activeContest.get();
        
        String eligibilityStatus;
        Color embedColor;
        
        if (p.isEligible()) {
            eligibilityStatus = "✅ **ELEGÍVEL** para o sorteio!";
            embedColor = Color.GREEN;
        } else {
            double remaining = contest.getRequiredHours() - p.getTotalHours();
            eligibilityStatus = String.format("❌ **Não elegível** (faltam %.1f horas)", remaining);
            embedColor = Color.ORANGE;
        }
        
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(embedColor)
                .title("📊 Suas Horas de Patrulha")
                .description(String.format(
                        "**Concurso:** %s\n\n" +
                        "⏰ **Total de horas:** %.1f / %d\n" +
                        "🕐 **Horas da tarde:** %.1f\n" +
                        "🌙 **Horas da noite:** %.1f\n\n" +
                        "%s\n\n" +
                        "%s%s",
                        contest.getTitle(),
                        p.getTotalHours(),
                        contest.getRequiredHours(),
                        p.getTotalAfternoonHours(),
                        p.getTotalNightHours(),
                        eligibilityStatus,
                        p.isAfternoonEligible() ? "🕐 Elegível para prêmios da tarde\n" : "",
                        p.isNightEligible() ? "🌙 Elegível para VIP da noite\n" : ""))
                .build();
        
        return event.reply().withEmbeds(embed).withEphemeral(true);
    }
    
    private Mono<Void> handleLeaderboard(ChatInputInteractionEvent event, String guildId) {
        Optional<PatrolContest> activeContest = patrolContestService.getActiveContest(guildId);
        
        if (activeContest.isEmpty()) {
            return event.reply("❌ Não há concurso ativo no momento.").withEphemeral(true);
        }
        
        List<PatrolParticipant> participants = patrolContestService
                .getEligibleParticipants(activeContest.get().getId());
        
        if (participants.isEmpty()) {
            return event.reply("📊 Ainda não há participantes elegíveis no concurso.").withEphemeral(true);
        }
        
        // Sort by total hours descending
        participants.sort((a, b) -> Double.compare(b.getTotalHours(), a.getTotalHours()));
        
        StringBuilder leaderboard = new StringBuilder();
        for (int i = 0; i < Math.min(10, participants.size()); i++) {
            PatrolParticipant p = participants.get(i);
            leaderboard.append(String.format(
                    "%d. **%s** - %.1fh (🕐%.1fh 🌙%.1fh)\n",
                    i + 1,
                    p.getNickname() != null ? p.getNickname() : p.getUsername(),
                    p.getTotalHours(),
                    p.getTotalAfternoonHours(),
                    p.getTotalNightHours()));
        }
        
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.of(255, 215, 0)) // Gold color
                .title("🏆 Ranking de Patrulha")
                .description(String.format(
                        "**%s**\n\n" +
                        "**Top 10 Participantes Elegíveis:**\n\n%s\n" +
                        "Total de elegíveis: %d",
                        activeContest.get().getTitle(),
                        leaderboard.toString(),
                        participants.size()))
                .build();
        
        return event.reply().withEmbeds(embed);
    }
    
    private String getOptionValue(List<ApplicationCommandInteractionOption> options, String name) {
        return options.stream()
                .filter(opt -> opt.getName().equals(name))
                .findFirst()
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse(null);
    }
    
    private Mono<Void> handleDrawWinners(ChatInputInteractionEvent event, String guildId) {
        Optional<PatrolContest> activeContest = patrolContestService.getActiveContest(guildId);
        
        if (activeContest.isEmpty()) {
            return event.reply("❌ Não há concurso ativo no momento.").withEphemeral(true);
        }
        
        PatrolContest contest = activeContest.get();
        
        // Check if winners were already drawn
        if (patrolLotteryService.hasDrawnWinners(contest.getId())) {
            return event.reply("⚠️ O sorteio já foi realizado para este concurso. Use `/patrol-contest winners` para ver os resultados.").withEphemeral(true);
        }
        
        try {
            // Perform the lottery
            PatrolLotteryService.LotteryResults results = patrolLotteryService.performFullLottery(contest);
            
            StringBuilder description = new StringBuilder();
            description.append(String.format("**%s**\n\n", contest.getTitle()));
            
            // Afternoon winners
            if (!results.getAfternoonWinners().isEmpty()) {
                description.append("🏆 **Ganhadores da Tarde** (Diamantes/Skins):\n");
                for (PatrolParticipant winner : results.getAfternoonWinners()) {
                    description.append(String.format("• **%s** - %.1fh tarde, %.1fh total\n", 
                            winner.getNickname() != null ? winner.getNickname() : winner.getUsername(),
                            winner.getTotalAfternoonHours(), winner.getTotalHours()));
                }
                description.append("\n");
            }
            
            // Night VIP winners
            if (!results.getNightVipWinners().isEmpty()) {
                description.append("💎 **Ganhadores VIP Diamante**:\n");
                for (PatrolParticipant winner : results.getNightVipWinners()) {
                    description.append(String.format("• **%s** - %.1fh noite, %.1fh total\n", 
                            winner.getNickname() != null ? winner.getNickname() : winner.getUsername(),
                            winner.getTotalNightHours(), winner.getTotalHours()));
                }
                description.append("\n");
            }
            
            if (results.getTotalWinners() == 0) {
                description.append("❌ Não há participantes elegíveis para o sorteio.");
            } else {
                description.append(String.format("🎉 **Total de ganhadores:** %d", results.getTotalWinners()));
            }
            
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .color(Color.of(255, 215, 0)) // Gold
                    .title("🎊 Resultado do Sorteio!")
                    .description(description.toString())
                    .timestamp(java.time.Instant.now())
                    .build();
            
            return event.reply().withEmbeds(embed);
            
        } catch (Exception e) {
            log.error("Error performing lottery", e);
            return event.reply("❌ Erro ao realizar o sorteio: " + e.getMessage()).withEphemeral(true);
        }
    }
    
    private Mono<Void> handleShowWinners(ChatInputInteractionEvent event, String guildId) {
        Optional<PatrolContest> activeContest = patrolContestService.getActiveContest(guildId);
        
        if (activeContest.isEmpty()) {
            return event.reply("❌ Não há concurso ativo no momento.").withEphemeral(true);
        }
        
        PatrolContest contest = activeContest.get();
        List<PatrolParticipant> afternoonWinners = patrolLotteryService.getAfternoonWinners(contest.getId());
        List<PatrolParticipant> nightVipWinners = patrolLotteryService.getNightVipWinners(contest.getId());
        
        if (afternoonWinners.isEmpty() && nightVipWinners.isEmpty()) {
            return event.reply("❌ O sorteio ainda não foi realizado para este concurso.").withEphemeral(true);
        }
        
        StringBuilder description = new StringBuilder();
        description.append(String.format("**%s**\n\n", contest.getTitle()));
        
        // Show afternoon winners
        if (!afternoonWinners.isEmpty()) {
            description.append("🏆 **Ganhadores da Tarde** (Diamantes/Skins):\n");
            for (PatrolParticipant winner : afternoonWinners) {
                description.append(String.format("• **%s** - %.1fh tarde\n", 
                        winner.getNickname() != null ? winner.getNickname() : winner.getUsername(),
                        winner.getTotalAfternoonHours()));
            }
            description.append("\n");
        }
        
        // Show night VIP winners
        if (!nightVipWinners.isEmpty()) {
            description.append("💎 **Ganhadores VIP Diamante**:\n");
            for (PatrolParticipant winner : nightVipWinners) {
                description.append(String.format("• **%s** - %.1fh noite\n", 
                        winner.getNickname() != null ? winner.getNickname() : winner.getUsername(),
                        winner.getTotalNightHours()));
            }
        }
        
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.of(255, 215, 0)) // Gold
                .title("🏆 Ganhadores do Concurso")
                .description(description.toString())
                .build();
        
        return event.reply().withEmbeds(embed);
    }
}