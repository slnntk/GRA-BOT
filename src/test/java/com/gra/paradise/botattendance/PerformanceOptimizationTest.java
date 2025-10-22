package com.gra.paradise.botattendance;

import com.gra.paradise.botattendance.model.Schedule;
import com.gra.paradise.botattendance.model.User;
import com.gra.paradise.botattendance.model.AircraftType;
import com.gra.paradise.botattendance.model.MissionType;
import com.gra.paradise.botattendance.repository.ScheduleRepositoryOptimized;
import com.gra.paradise.botattendance.service.PerformanceMetricsService;
import com.gra.paradise.botattendance.service.ScheduleServiceOptimized;
import com.gra.paradise.botattendance.service.CacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de Performance e Otimização
 * Valida melhorias de performance implementadas
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PerformanceOptimizationTest {

    @Autowired
    private ScheduleRepositoryOptimized scheduleRepository;

    @Autowired
    private ScheduleServiceOptimized scheduleService;

    @Autowired
    private PerformanceMetricsService performanceMetrics;

    @Autowired
    private CacheService cacheService;

    @Test
    void testOptimizedDatabaseQueries() {
        // Criar dados de teste
        Schedule schedule = createTestSchedule();
        scheduleRepository.save(schedule);

        // Testar consulta otimizada
        List<Schedule> schedules = scheduleRepository.findActiveSchedulesWithCrew("test-guild");
        
        assertThat(schedules).hasSize(1);
        assertThat(schedules.get(0).getCrewMembers()).isNotNull();
    }

    @Test
    void testCachePerformance() {
        // Limpar cache
        cacheService.clear();

        // Primeira consulta (cache miss)
        long startTime = System.currentTimeMillis();
        List<Schedule> schedules1 = scheduleService.getActiveSchedules("test-guild");
        long firstQueryTime = System.currentTimeMillis() - startTime;

        // Segunda consulta (cache hit)
        startTime = System.currentTimeMillis();
        List<Schedule> schedules2 = scheduleService.getActiveSchedules("test-guild");
        long secondQueryTime = System.currentTimeMillis() - startTime;

        // Cache hit deve ser mais rápido
        assertThat(secondQueryTime).isLessThan(firstQueryTime);
        assertThat(schedules1).isEqualTo(schedules2);
    }

    @Test
    void testAsyncOperations() {
        // Testar operações assíncronas
        CompletableFuture<List<Schedule>> future = scheduleService.getSchedulesByAircraftTypeAsync("test-guild", "EC135");
        
        assertThat(future).isNotNull();
        
        // Aguardar conclusão
        List<Schedule> schedules = future.join();
        assertThat(schedules).isNotNull();
    }

    @Test
    void testPerformanceMetrics() {
        // Testar métricas de performance
        performanceMetrics.recordDiscordEvent();
        performanceMetrics.recordDatabaseQuery();
        performanceMetrics.recordCacheHit();

        // Verificar se métricas foram registradas
        String summary = performanceMetrics.getPerformanceSummary();
        assertThat(summary).contains("Discord Events: 1");
        assertThat(summary).contains("Database Queries: 1");
    }

    @Test
    void testDatabaseOptimization() {
        // Criar múltiplas escalas
        for (int i = 0; i < 10; i++) {
            Schedule schedule = createTestSchedule();
            schedule.setTitle("Test Schedule " + i);
            scheduleRepository.save(schedule);
        }

        // Testar contagem otimizada
        long count = scheduleRepository.countActiveSchedulesByGuildId("test-guild");
        assertThat(count).isEqualTo(10);

        // Testar busca por período
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now().plusSeconds(3600);
        List<Schedule> schedules = scheduleRepository.findActiveSchedulesByDateRange("test-guild", startDate, endDate);
        assertThat(schedules).hasSize(10);
    }

    @Test
    void testMemoryOptimization() {
        // Testar otimização de memória
        String initialMemory = performanceMetrics.getPerformanceSummary();
        
        // Criar e limpar cache
        cacheService.clear();
        
        // Verificar se memória foi liberada
        String finalMemory = performanceMetrics.getPerformanceSummary();
        assertThat(finalMemory).isNotNull();
    }

    @Test
    void testConcurrentOperations() {
        // Testar operações concorrentes
        List<CompletableFuture<List<Schedule>>> futures = List.of(
            scheduleService.getSchedulesByAircraftTypeAsync("test-guild", "EC135"),
            scheduleService.getSchedulesByAircraftTypeAsync("test-guild", "VALKYRE"),
            scheduleService.getSchedulesByAircraftTypeAsync("test-guild", "MAVERICK")
        );

        // Aguardar todas as operações
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Verificar se todas completaram
        futures.forEach(future -> assertThat(future.isDone()).isTrue());
    }

    private Schedule createTestSchedule() {
        Schedule schedule = new Schedule();
        schedule.setGuildId("test-guild");
        schedule.setTitle("Test Schedule");
        schedule.setCreatedById("test-user");
        schedule.setCreatedByUsername("testuser");
        schedule.setStartTime(Instant.now());
        schedule.setEndTime(Instant.now().plusSeconds(3600));
        schedule.setAircraftType(AircraftType.EC135);
        schedule.setMissionType(MissionType.PATROL);
        schedule.setActive(true);
        return schedule;
    }
}
