package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.model.Schedule;
import com.gra.paradise.botattendance.repository.ScheduleRepositoryOptimized;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço Otimizado para Schedule
 * Usa cache, consultas otimizadas e operações assíncronas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceOptimized {

    private final ScheduleRepositoryOptimized scheduleRepository;
    private final PerformanceMetricsService performanceMetrics;
    private final CacheService cacheService;

    /**
     * Busca escalas ativas com cache inteligente
     */
    @Cacheable(value = "activeSchedules", key = "#guildId")
    public List<Schedule> getActiveSchedules(String guildId) {
        Timer.Sample sample = performanceMetrics.startDatabaseTimer();
        try {
            List<Schedule> schedules = scheduleRepository.findActiveSchedulesWithCrew(guildId);
            performanceMetrics.recordDatabaseQuery();
            return schedules;
        } finally {
            performanceMetrics.recordDatabaseQueryTime(sample);
        }
    }

    /**
     * Busca escala específica com cache
     */
    @Cacheable(value = "schedule", key = "#id + '_' + #guildId")
    public Optional<Schedule> getScheduleById(Long id, String guildId) {
        Timer.Sample sample = performanceMetrics.startDatabaseTimer();
        try {
            Optional<Schedule> schedule = scheduleRepository.findByIdAndGuildIdWithRelations(id, guildId);
            performanceMetrics.recordDatabaseQuery();
            return schedule;
        } finally {
            performanceMetrics.recordDatabaseQueryTime(sample);
        }
    }

    /**
     * Conta escalas ativas de forma otimizada
     */
    @Cacheable(value = "scheduleCount", key = "#guildId")
    public long countActiveSchedules(String guildId) {
        Timer.Sample sample = performanceMetrics.startDatabaseTimer();
        try {
            long count = scheduleRepository.countActiveSchedulesByGuildId(guildId);
            performanceMetrics.recordDatabaseQuery();
            return count;
        } finally {
            performanceMetrics.recordDatabaseQueryTime(sample);
        }
    }

    /**
     * Busca escalas por aeronave de forma assíncrona
     */
    public CompletableFuture<List<Schedule>> getSchedulesByAircraftTypeAsync(String guildId, String aircraftType) {
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = performanceMetrics.startDatabaseTimer();
            try {
                List<Schedule> schedules = scheduleRepository.findActiveSchedulesByAircraftType(guildId, aircraftType);
                performanceMetrics.recordDatabaseQuery();
                return schedules;
            } finally {
                performanceMetrics.recordDatabaseQueryTime(sample);
            }
        });
    }

    /**
     * Busca escalas por período
     */
    @Cacheable(value = "schedulesByDate", key = "#guildId + '_' + #startDate + '_' + #endDate")
    public List<Schedule> getSchedulesByDateRange(String guildId, Instant startDate, Instant endDate) {
        Timer.Sample sample = performanceMetrics.startDatabaseTimer();
        try {
            List<Schedule> schedules = scheduleRepository.findActiveSchedulesByDateRange(guildId, startDate, endDate);
            performanceMetrics.recordDatabaseQuery();
            return schedules;
        } finally {
            performanceMetrics.recordDatabaseQueryTime(sample);
        }
    }

    /**
     * Busca escalas que expiram em breve
     */
    public List<Schedule> getSchedulesExpiringSoon() {
        Timer.Sample sample = performanceMetrics.startDatabaseTimer();
        try {
            Instant expirationTime = Instant.now().plusSeconds(3600); // 1 hora
            List<Schedule> schedules = scheduleRepository.findSchedulesExpiringSoon(expirationTime);
            performanceMetrics.recordDatabaseQuery();
            return schedules;
        } finally {
            performanceMetrics.recordDatabaseQueryTime(sample);
        }
    }

    /**
     * Busca escalas por usuário
     */
    @Cacheable(value = "userSchedules", key = "#guildId + '_' + #discordId")
    public List<Schedule> getSchedulesByUser(String guildId, String discordId) {
        Timer.Sample sample = performanceMetrics.startDatabaseTimer();
        try {
            List<Schedule> schedules = scheduleRepository.findActiveSchedulesByUser(guildId, discordId);
            performanceMetrics.recordDatabaseQuery();
            return schedules;
        } finally {
            performanceMetrics.recordDatabaseQueryTime(sample);
        }
    }

    /**
     * Salva escala e limpa cache
     */
    @Transactional
    @CacheEvict(value = {"activeSchedules", "scheduleCount", "schedulesByDate", "userSchedules"}, allEntries = true)
    public Schedule saveSchedule(Schedule schedule) {
        Timer.Sample sample = performanceMetrics.startDatabaseTimer();
        try {
            Schedule saved = scheduleRepository.save(schedule);
            performanceMetrics.recordDatabaseQuery();
            return saved;
        } finally {
            performanceMetrics.recordDatabaseQueryTime(sample);
        }
    }

    /**
     * Atualiza escala e limpa cache
     */
    @Transactional
    @CacheEvict(value = {"activeSchedules", "scheduleCount", "schedulesByDate", "userSchedules", "schedule"}, allEntries = true)
    public Schedule updateSchedule(Schedule schedule) {
        Timer.Sample sample = performanceMetrics.startDatabaseTimer();
        try {
            Schedule updated = scheduleRepository.save(schedule);
            performanceMetrics.recordDatabaseQuery();
            return updated;
        } finally {
            performanceMetrics.recordDatabaseQueryTime(sample);
        }
    }

    /**
     * Remove escala e limpa cache
     */
    @Transactional
    @CacheEvict(value = {"activeSchedules", "scheduleCount", "schedulesByDate", "userSchedules", "schedule"}, allEntries = true)
    public void deleteSchedule(Long id) {
        Timer.Sample sample = performanceMetrics.startDatabaseTimer();
        try {
            scheduleRepository.deleteById(id);
            performanceMetrics.recordDatabaseQuery();
        } finally {
            performanceMetrics.recordDatabaseQueryTime(sample);
        }
    }

    /**
     * Limpa cache manualmente
     */
    public void clearCache() {
        cacheService.clear();
        log.info("Cache cleared manually");
    }
}
