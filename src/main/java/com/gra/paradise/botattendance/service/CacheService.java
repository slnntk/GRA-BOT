package com.gra.paradise.botattendance.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Timer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Serviço de Cache Otimizado
 * Gerencia cache inteligente com TTL e invalidação automática
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final Cache<String, Object> discordMessageCache;
    private final Cache<String, Object> guildConfigCache;
    private final Cache<String, Object> userCache;
    private final PerformanceMetricsService performanceMetrics;

    /**
     * Obtém valor do cache ou executa supplier se não encontrado
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        Timer.Sample sample = performanceMetrics.startCacheTimer();
        try {
            Object value = getCacheForType(type).getIfPresent(key);
            if (value != null) {
                performanceMetrics.recordCacheHit();
                return Optional.of(type.cast(value));
            } else {
                performanceMetrics.recordCacheMiss();
                return Optional.empty();
            }
        } finally {
            performanceMetrics.recordCacheAccessTime(sample);
        }
    }

    /**
     * Obtém valor do cache ou executa supplier assíncrono
     */
    public <T> CompletableFuture<T> getAsync(String key, Class<T> type, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<T> cached = get(key, type);
            if (cached.isPresent()) {
                return cached.get();
            }
            
            T value = supplier.get();
            put(key, value);
            return value;
        });
    }

    /**
     * Armazena valor no cache
     */
    public void put(String key, Object value) {
        if (value != null) {
            getCacheForValue(value).put(key, value);
            performanceMetrics.recordCacheOperation();
        }
    }

    /**
     * Remove valor do cache
     */
    public void evict(String key) {
        discordMessageCache.invalidate(key);
        guildConfigCache.invalidate(key);
        userCache.invalidate(key);
    }

    /**
     * Limpa todo o cache
     */
    public void clear() {
        discordMessageCache.invalidateAll();
        guildConfigCache.invalidateAll();
        userCache.invalidateAll();
        log.info("All caches cleared");
    }

    /**
     * Obtém estatísticas do cache
     */
    public String getCacheStats() {
        return String.format(
            "Cache Statistics:\n" +
            "- Discord Messages: %d entries, %d hits, %d misses\n" +
            "- Guild Configs: %d entries, %d hits, %d misses\n" +
            "- Users: %d entries, %d hits, %d misses",
            discordMessageCache.estimatedSize(),
            (int) discordMessageCache.stats().hitCount(),
            (int) discordMessageCache.stats().missCount(),
            guildConfigCache.estimatedSize(),
            (int) guildConfigCache.stats().hitCount(),
            (int) guildConfigCache.stats().missCount(),
            userCache.estimatedSize(),
            (int) userCache.stats().hitCount(),
            (int) userCache.stats().missCount()
        );
    }

    /**
     * Determina qual cache usar baseado no tipo
     */
    private Cache<String, Object> getCacheForType(Class<?> type) {
        if (type.getSimpleName().contains("Message") || type.getSimpleName().contains("Embed")) {
            return discordMessageCache;
        } else if (type.getSimpleName().contains("Guild") || type.getSimpleName().contains("Config")) {
            return guildConfigCache;
        } else {
            return userCache;
        }
    }

    /**
     * Determina qual cache usar baseado no valor
     */
    private Cache<String, Object> getCacheForValue(Object value) {
        String className = value.getClass().getSimpleName();
        if (className.contains("Message") || className.contains("Embed")) {
            return discordMessageCache;
        } else if (className.contains("Guild") || className.contains("Config")) {
            return guildConfigCache;
        } else {
            return userCache;
        }
    }
}
