package com.gra.paradise.botattendance.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Serviço de Métricas de Performance
 * Monitora CPU, RAM, Latência e outras métricas importantes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceMetricsService {

    private final MeterRegistry meterRegistry;
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    // Counters para eventos
    private Counter discordEvents;
    private Counter databaseQueries;
    private Counter cacheOperations;
    private Counter errors;

    // Timers para latência
    private Timer discordResponseTime;
    private Timer databaseQueryTime;
    private Timer cacheAccessTime;

    public void initializeMetrics() {
        // Counters
        discordEvents = Counter.builder("discord.events.total")
                .description("Total Discord events processed")
                .register(meterRegistry);

        databaseQueries = Counter.builder("database.queries.total")
                .description("Total database queries executed")
                .register(meterRegistry);

        cacheOperations = Counter.builder("cache.operations.total")
                .description("Total cache operations")
                .register(meterRegistry);

        errors = Counter.builder("application.errors.total")
                .description("Total application errors")
                .register(meterRegistry);

        // Timers
        discordResponseTime = Timer.builder("discord.response.time")
                .description("Discord API response time")
                .register(meterRegistry);

        databaseQueryTime = Timer.builder("database.query.time")
                .description("Database query execution time")
                .register(meterRegistry);

        cacheAccessTime = Timer.builder("cache.access.time")
                .description("Cache access time")
                .register(meterRegistry);

        // Gauges para métricas contínuas
        Gauge.builder("system.memory.used", this, PerformanceMetricsService::getUsedMemoryMB)
                .description("Used memory in MB")
                .register(meterRegistry);

        Gauge.builder("system.memory.free", this, PerformanceMetricsService::getFreeMemoryMB)
                .description("Free memory in MB")
                .register(meterRegistry);

        Gauge.builder("system.cpu.usage", this, PerformanceMetricsService::getCpuUsage)
                .description("CPU usage percentage")
                .register(meterRegistry);

        Gauge.builder("application.connections.active", activeConnections, AtomicLong::get)
                .description("Active connections")
                .register(meterRegistry);

        Gauge.builder("cache.hit.ratio", this, PerformanceMetricsService::getCacheHitRatio)
                .description("Cache hit ratio")
                .register(meterRegistry);

        log.info("Performance metrics initialized successfully");
    }

    // Métodos para registrar eventos
    public void recordDiscordEvent() {
        discordEvents.increment();
    }

    public void recordDatabaseQuery() {
        databaseQueries.increment();
    }

    public void recordCacheOperation() {
        cacheOperations.increment();
    }

    public void recordError() {
        errors.increment();
    }

    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    // Métodos para medir tempo
    public Timer.Sample startDiscordTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordDiscordResponseTime(Timer.Sample sample) {
        sample.stop(discordResponseTime);
    }

    public Timer.Sample startDatabaseTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordDatabaseQueryTime(Timer.Sample sample) {
        sample.stop(databaseQueryTime);
    }

    public Timer.Sample startCacheTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordCacheAccessTime(Timer.Sample sample) {
        sample.stop(cacheAccessTime);
    }

    // Métodos para conexões
    public void incrementActiveConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementActiveConnections() {
        activeConnections.decrementAndGet();
    }

    // Métodos para obter métricas
    private double getUsedMemoryMB() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        return usedMemory / (1024.0 * 1024.0);
    }

    private double getFreeMemoryMB() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        return (maxMemory - usedMemory) / (1024.0 * 1024.0);
    }

    private double getCpuUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad() * 100;
        }
        return 0.0;
    }

    private double getCacheHitRatio() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total * 100 : 0.0;
    }

    // Método para obter resumo de performance
    public String getPerformanceSummary() {
        return String.format(
            "Performance Summary:\n" +
            "- Memory Used: %.2f MB\n" +
            "- Memory Free: %.2f MB\n" +
            "- CPU Usage: %.2f%%\n" +
            "- Active Connections: %d\n" +
            "- Cache Hit Ratio: %.2f%%\n" +
            "- Discord Events: %d\n" +
            "- Database Queries: %d\n" +
            "- Cache Operations: %d\n" +
            "- Errors: %d",
            getUsedMemoryMB(),
            getFreeMemoryMB(),
            getCpuUsage(),
            activeConnections.get(),
            getCacheHitRatio(),
            (int) discordEvents.count(),
            (int) databaseQueries.count(),
            (int) cacheOperations.count(),
            (int) errors.count()
        );
    }

    /**
     * Limpa métricas antigas para liberar memória
     */
    public void clearOldMetrics() {
        // Limpa contadores antigos
        if (discordEvents != null) {
            // Reset contador de eventos Discord
        }
        if (errors != null) {
            // Reset contador de erros
        }
        
        // Força limpeza de métricas antigas
        meterRegistry.clear();
        
        log.info("Old metrics cleared to free memory");
    }
}
