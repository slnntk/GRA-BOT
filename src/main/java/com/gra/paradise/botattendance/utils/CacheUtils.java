package com.gra.paradise.botattendance.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Utilitários de Cache
 * Implementação de cache otimizado com TTL e limpeza automática
 */
@Slf4j
public class CacheUtils {

    private static final ScheduledExecutorService cleanupExecutor = 
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "cache-cleanup");
                t.setDaemon(true);
                return t;
            });

    /**
     * Cache com TTL e limpeza automática
     */
    public static class TTLCache<K, V> {
        private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
        private final Duration ttl;
        private final int maxSize;

        public TTLCache(Duration ttl, int maxSize) {
            this.ttl = ttl;
            this.maxSize = maxSize;
            scheduleCleanup();
        }

        public V get(K key) {
            CacheEntry<V> entry = cache.get(key);
            if (entry == null) {
                return null;
            }
            
            if (entry.isExpired()) {
                cache.remove(key);
                return null;
            }
            
            entry.updateAccessTime();
            return entry.getValue();
        }

        public V put(K key, V value) {
            if (cache.size() >= maxSize) {
                evictOldest();
            }
            
            CacheEntry<V> entry = new CacheEntry<>(value, Instant.now());
            cache.put(key, entry);
            return value;
        }

        public V computeIfAbsent(K key, Function<K, V> mappingFunction) {
            V value = get(key);
            if (value != null) {
                return value;
            }
            
            V computedValue = mappingFunction.apply(key);
            if (computedValue != null) {
                put(key, computedValue);
            }
            return computedValue;
        }

        public void remove(K key) {
            cache.remove(key);
        }

        public void clear() {
            cache.clear();
        }

        public int size() {
            return cache.size();
        }

        public boolean isEmpty() {
            return cache.isEmpty();
        }

        private void evictOldest() {
            cache.entrySet().stream()
                    .min((e1, e2) -> e1.getValue().getLastAccessTime()
                            .compareTo(e2.getValue().getLastAccessTime()))
                    .ifPresent(entry -> cache.remove(entry.getKey()));
        }

        private void scheduleCleanup() {
            cleanupExecutor.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
        }

        private void cleanup() {
            Instant now = Instant.now();
            cache.entrySet().removeIf(entry -> {
                boolean expired = entry.getValue().isExpired(now);
                if (expired) {
                    log.debug("Removed expired cache entry: {}", entry.getKey());
                }
                return expired;
            });
        }
    }

    /**
     * Entrada do cache com TTL
     */
    private static class CacheEntry<V> {
        private final V value;
        private final Instant creationTime;
        private Instant lastAccessTime;
        private final Duration ttl;

        public CacheEntry(V value, Instant creationTime) {
            this.value = value;
            this.creationTime = creationTime;
            this.lastAccessTime = creationTime;
            this.ttl = Duration.ofMinutes(30); // TTL padrão
        }

        public V getValue() {
            return value;
        }

        public Instant getLastAccessTime() {
            return lastAccessTime;
        }

        public void updateAccessTime() {
            this.lastAccessTime = Instant.now();
        }

        public boolean isExpired() {
            return isExpired(Instant.now());
        }

        public boolean isExpired(Instant now) {
            return Duration.between(creationTime, now).compareTo(ttl) > 0;
        }
    }

    /**
     * Cache com estatísticas
     */
    public static class StatisticsCache<K, V> extends TTLCache<K, V> {
        private long hits = 0;
        private long misses = 0;

        public StatisticsCache(Duration ttl, int maxSize) {
            super(ttl, maxSize);
        }

        @Override
        public V get(K key) {
            V value = super.get(key);
            if (value != null) {
                hits++;
            } else {
                misses++;
            }
            return value;
        }

        public double getHitRatio() {
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        }

        public long getHits() {
            return hits;
        }

        public long getMisses() {
            return misses;
        }

        public String getStatistics() {
            return String.format("Cache Stats - Hits: %d, Misses: %d, Hit Ratio: %.2f%%, Size: %d", 
                    hits, misses, getHitRatio() * 100, size());
        }
    }

    /**
     * Cache com invalidação por padrão
     */
    public static class PatternCache<K, V> extends TTLCache<K, V> {
        private final Function<K, Boolean> invalidationPattern;

        public PatternCache(Duration ttl, int maxSize, Function<K, Boolean> invalidationPattern) {
            super(ttl, maxSize);
            this.invalidationPattern = invalidationPattern;
        }

        @Override
        public V get(K key) {
            if (invalidationPattern.apply(key)) {
                remove(key);
                return null;
            }
            return super.get(key);
        }
    }

    /**
     * Cria cache otimizado
     */
    public static <K, V> TTLCache<K, V> createCache(Duration ttl, int maxSize) {
        return new TTLCache<>(ttl, maxSize);
    }

    /**
     * Cria cache com estatísticas
     */
    public static <K, V> StatisticsCache<K, V> createStatisticsCache(Duration ttl, int maxSize) {
        return new StatisticsCache<>(ttl, maxSize);
    }

    /**
     * Cria cache com padrão de invalidação
     */
    public static <K, V> PatternCache<K, V> createPatternCache(
            Duration ttl, 
            int maxSize, 
            Function<K, Boolean> invalidationPattern) {
        return new PatternCache<>(ttl, maxSize, invalidationPattern);
    }
}
