package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.model.*;
import com.gra.paradise.botattendance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.gra.paradise.botattendance.config.DiscordConfig.FORTALEZA_ZONE;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatrolContestService {
    
    private final PatrolContestRepository patrolContestRepository;
    private final PatrolParticipantRepository patrolParticipantRepository;
    private final PatrolHoursRepository patrolHoursRepository;
    private final ScheduleRepository scheduleRepository;
    
    @Transactional
    public PatrolContest createContest(String guildId, String title, String description, 
                                     LocalDate startDate, LocalDate endDate, String createdBy) {
        PatrolContest contest = new PatrolContest();
        contest.setGuildId(guildId);
        contest.setTitle(title);
        contest.setDescription(description);
        contest.setStartDate(startDate.atStartOfDay(FORTALEZA_ZONE).toInstant());
        contest.setEndDate(endDate.atTime(23, 59, 59).atZone(FORTALEZA_ZONE).toInstant());
        contest.setCreatedBy(createdBy);
        contest.setActive(true);
        
        PatrolContest saved = patrolContestRepository.save(contest);
        log.info("Patrol contest created: {} (ID: {})", saved.getTitle(), saved.getId());
        return saved;
    }
    
    @Transactional(readOnly = true)
    public Optional<PatrolContest> getActiveContest(String guildId) {
        return patrolContestRepository.findActiveContestForGuild(guildId, Instant.now());
    }
    
    @Transactional
    public void processPatrolSchedule(Schedule schedule) {
        if (schedule.getMissionType() != MissionType.PATROL || 
            schedule.getEndTime() == null || 
            !schedule.isActive()) {
            return;
        }
        
        Optional<PatrolContest> activeContest = getActiveContest(schedule.getGuildId());
        if (activeContest.isEmpty()) {
            return;
        }
        
        PatrolContest contest = activeContest.get();
        
        // Process patrol hours for creator
        processUserPatrolHours(contest, schedule.getCreatedById(), 
                              schedule.getCreatedByUsername(), schedule);
        
        // Process patrol hours for crew members
        if (schedule.getCrewMembers() != null) {
            for (User crewMember : schedule.getCrewMembers()) {
                processUserPatrolHours(contest, crewMember.getDiscordId(), 
                                      crewMember.getNickname(), schedule);
            }
        }
    }
    
    private void processUserPatrolHours(PatrolContest contest, String discordId, 
                                       String username, Schedule schedule) {
        if (schedule.getStartTime() == null || schedule.getEndTime() == null) {
            return;
        }
        
        ZonedDateTime start = schedule.getStartTime().atZone(FORTALEZA_ZONE);
        ZonedDateTime end = schedule.getEndTime().atZone(FORTALEZA_ZONE);
        
        // Check if patrol is within contest period
        if (start.toInstant().isBefore(contest.getStartDate()) || 
            start.toInstant().isAfter(contest.getEndDate())) {
            return;
        }
        
        LocalDate patrolDate = start.toLocalDate();
        
        // Check daily hours limit
        Double dailyHours = patrolHoursRepository.getTotalHoursForUserOnDate(
            contest.getId(), discordId, patrolDate);
        if (dailyHours != null && dailyHours >= contest.getMaxDailyHours()) {
            log.info("User {} already reached daily limit for {}", discordId, patrolDate);
            return;
        }
        
        // Calculate patrol hours and categorize by time periods
        PatrolHours patrolHours = calculatePatrolHours(contest, schedule, discordId, username);
        
        // Apply daily limit
        double remainingDaily = contest.getMaxDailyHours() - (dailyHours != null ? dailyHours : 0.0);
        if (patrolHours.getHours() > remainingDaily) {
            // Proportionally reduce hours
            double ratio = remainingDaily / patrolHours.getHours();
            patrolHours.setHours(remainingDaily);
            patrolHours.setAfternoonHours(patrolHours.getAfternoonHours() * ratio);
            patrolHours.setNightHours(patrolHours.getNightHours() * ratio);
        }
        
        patrolHoursRepository.save(patrolHours);
        
        // Update participant totals
        updateParticipantHours(contest, discordId, username);
        
        log.info("Processed patrol hours for user {}: {} total, {} afternoon, {} night", 
                discordId, patrolHours.getHours(), patrolHours.getAfternoonHours(), 
                patrolHours.getNightHours());
    }
    
    private PatrolHours calculatePatrolHours(PatrolContest contest, Schedule schedule, 
                                           String discordId, String username) {
        ZonedDateTime start = schedule.getStartTime().atZone(FORTALEZA_ZONE);
        ZonedDateTime end = schedule.getEndTime().atZone(FORTALEZA_ZONE);
        
        PatrolHours patrolHours = new PatrolHours();
        patrolHours.setContest(contest);
        patrolHours.setDiscordId(discordId);
        patrolHours.setUsername(username);
        patrolHours.setScheduleId(schedule.getId());
        patrolHours.setPatrolDate(start.toLocalDate());
        patrolHours.setStartTime(start.toLocalTime());
        patrolHours.setEndTime(end.toLocalTime());
        
        Duration totalDuration = Duration.between(start, end);
        double totalHours = totalDuration.toMinutes() / 60.0;
        patrolHours.setHours(totalHours);
        
        // Calculate hours in specific periods
        double afternoonHours = calculatePeriodHours(start, end, 
            contest.getAfternoonStart(), contest.getAfternoonEnd());
        double nightHours = calculatePeriodHours(start, end, 
            contest.getNightStart(), contest.getNightEnd());
        
        patrolHours.setAfternoonHours(afternoonHours);
        patrolHours.setNightHours(nightHours);
        
        // Determine primary period
        if (afternoonHours > nightHours && afternoonHours > 0) {
            patrolHours.setPrimaryPeriod(PatrolHours.PatrolPeriod.AFTERNOON);
        } else if (nightHours > afternoonHours && nightHours > 0) {
            patrolHours.setPrimaryPeriod(PatrolHours.PatrolPeriod.NIGHT);
        } else if (afternoonHours > 0 && nightHours > 0) {
            patrolHours.setPrimaryPeriod(PatrolHours.PatrolPeriod.MIXED);
        } else {
            patrolHours.setPrimaryPeriod(PatrolHours.PatrolPeriod.OTHER);
        }
        
        return patrolHours;
    }
    
    private double calculatePeriodHours(ZonedDateTime start, ZonedDateTime end, 
                                       String periodStart, String periodEnd) {
        LocalTime startTime = LocalTime.parse(periodStart);
        LocalTime endTime = LocalTime.parse(periodEnd);
        
        // Handle midnight crossing for night period
        if (endTime.equals(LocalTime.MIDNIGHT)) {
            endTime = LocalTime.of(23, 59, 59);
        }
        
        LocalDate date = start.toLocalDate();
        ZonedDateTime periodStartDt = date.atTime(startTime).atZone(FORTALEZA_ZONE);
        ZonedDateTime periodEndDt = date.atTime(endTime).atZone(FORTALEZA_ZONE);
        
        // Handle night period crossing midnight
        if (startTime.isAfter(endTime)) {
            periodEndDt = periodEndDt.plusDays(1);
        }
        
        // Calculate overlap
        ZonedDateTime overlapStart = start.isAfter(periodStartDt) ? start : periodStartDt;
        ZonedDateTime overlapEnd = end.isBefore(periodEndDt) ? end : periodEndDt;
        
        if (overlapStart.isBefore(overlapEnd)) {
            Duration overlap = Duration.between(overlapStart, overlapEnd);
            return overlap.toMinutes() / 60.0;
        }
        
        return 0.0;
    }
    
    @Transactional
    public void updateParticipantHours(PatrolContest contest, String discordId, String username) {
        PatrolParticipant participant = patrolParticipantRepository
            .findByContestIdAndDiscordId(contest.getId(), discordId)
            .orElseGet(() -> {
                PatrolParticipant newParticipant = new PatrolParticipant();
                newParticipant.setContest(contest);
                newParticipant.setDiscordId(discordId);
                newParticipant.setUsername(username);
                return newParticipant;
            });
        
        // Recalculate totals from PatrolHours
        Double afternoonTotal = patrolHoursRepository.getTotalAfternoonHoursForUser(
            contest.getId(), discordId);
        Double nightTotal = patrolHoursRepository.getTotalNightHoursForUser(
            contest.getId(), discordId);
        
        participant.setTotalAfternoonHours(afternoonTotal != null ? afternoonTotal : 0.0);
        participant.setTotalNightHours(nightTotal != null ? nightTotal : 0.0);
        participant.setTotalHours(participant.getTotalAfternoonHours() + participant.getTotalNightHours());
        
        patrolParticipantRepository.save(participant);
    }
    
    @Transactional(readOnly = true)
    public List<PatrolParticipant> getEligibleParticipants(Long contestId) {
        return patrolParticipantRepository.findByContestIdAndEligibleTrue(contestId);
    }
    
    @Transactional(readOnly = true)
    public Optional<PatrolParticipant> getParticipant(Long contestId, String discordId) {
        return patrolParticipantRepository.findByContestIdAndDiscordId(contestId, discordId);
    }
}