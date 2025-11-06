package com.gra.paradise.botattendance.discord.buttons.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Cache manager for description entries with TTL support.
 * Extracted from ScheduleInteractionHandler to separate cache management concerns.
 */
@Slf4j
@Component
public class DescriptionCacheManager {

    // Cache com TTL para evitar memory leak - auto cleanup após 10 minutos
    private final Map<String, CacheEntry> outrosDescriptionCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cacheCleanupExecutor = Executors.newScheduledThreadPool(1);

    // Classe interna para cache entry com timestamp para TTL
    private static class CacheEntry {
        final String description;
        final long timestamp;
        
        CacheEntry(String description) {
            this.description = description;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }

    public DescriptionCacheManager() {
        // Cleanup automático do cache a cada 5 minutos
        cacheCleanupExecutor.scheduleWithFixedDelay(this::cleanupExpiredCacheEntries, 
                5, 5, TimeUnit.MINUTES);
    }

    public void put(String sessionId, String description) {
        outrosDescriptionCache.put(sessionId, new CacheEntry(description));
    }

    public String get(String sessionId) {
        CacheEntry entry = outrosDescriptionCache.get(sessionId);
        return entry != null ? entry.description : null;
    }

    public void remove(String sessionId) {
        outrosDescriptionCache.remove(sessionId);
    }

    public boolean containsKey(String sessionId) {
        return outrosDescriptionCache.containsKey(sessionId);
    }

    private void cleanupExpiredCacheEntries() {
        long ttl = TimeUnit.MINUTES.toMillis(10); // TTL de 10 minutos
        outrosDescriptionCache.entrySet().removeIf(entry -> 
                entry.getValue().isExpired(ttl));
        log.debug("Cache cleanup executado. Entradas restantes: {}", outrosDescriptionCache.size());
    }

    @PreDestroy
    public void shutdown() {
        cacheCleanupExecutor.shutdown();
        try {
            if (!cacheCleanupExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                cacheCleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cacheCleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}