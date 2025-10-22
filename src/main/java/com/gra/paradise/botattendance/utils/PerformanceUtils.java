package com.gra.paradise.botattendance.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Utilitários de Performance
 * Ferramentas para otimização e monitoramento de performance
 */
@Slf4j
public class PerformanceUtils {

    /**
     * Executa operação com timeout e fallback
     */
    public static <T> CompletableFuture<T> executeWithTimeout(
            Supplier<T> operation, 
            Duration timeout, 
            T fallbackValue) {
        
        return CompletableFuture.supplyAsync(operation)
                .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> {
                    log.warn("Operation timed out after {}ms, using fallback", timeout.toMillis());
                    return fallbackValue;
                });
    }

    /**
     * Mede tempo de execução de uma operação
     */
    public static <T> T measureExecutionTime(String operationName, Supplier<T> operation) {
        Instant start = Instant.now();
        try {
            T result = operation.get();
            Duration duration = Duration.between(start, Instant.now());
            log.debug("Operation '{}' completed in {}ms", operationName, duration.toMillis());
            return result;
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            log.error("Operation '{}' failed after {}ms", operationName, duration.toMillis(), e);
            throw e;
        }
    }

    /**
     * Executa operação assíncrona com retry
     */
    public static <T> CompletableFuture<T> executeWithRetry(
            Supplier<T> operation, 
            int maxRetries, 
            Duration delayBetweenRetries) {
        
        return CompletableFuture.supplyAsync(() -> {
            Exception lastException = null;
            
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    return operation.get();
                } catch (Exception e) {
                    lastException = e;
                    if (attempt < maxRetries) {
                        log.warn("Attempt {} failed, retrying in {}ms", attempt, delayBetweenRetries.toMillis());
                        try {
                            Thread.sleep(delayBetweenRetries.toMillis());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted during retry", ie);
                        }
                    }
                }
            }
            
            throw new RuntimeException("Operation failed after " + maxRetries + " attempts", lastException);
        });
    }

    /**
     * Executa operações em paralelo com limite de concorrência
     */
    public static <T> CompletableFuture<List<T>> executeInParallel(
            List<Supplier<T>> operations, 
            int maxConcurrency) {
        
        if (operations.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        
        List<CompletableFuture<T>> futures = operations.stream()
                .map(CompletableFuture::supplyAsync)
                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    /**
     * Cria cache com TTL personalizado
     */
    public static <K, V> java.util.Map<K, V> createCacheWithTTL(Duration ttl) {
        return new java.util.concurrent.ConcurrentHashMap<K, V>() {
            private final java.util.Map<K, Instant> timestamps = new java.util.concurrent.ConcurrentHashMap<>();
            
            @Override
            public V get(Object key) {
                Instant timestamp = timestamps.get(key);
                if (timestamp != null && Duration.between(timestamp, Instant.now()).compareTo(ttl) > 0) {
                    remove(key);
                    timestamps.remove(key);
                    return null;
                }
                return super.get(key);
            }
            
            @Override
            public V put(K key, V value) {
                timestamps.put(key, Instant.now());
                return super.put(key, value);
            }
        };
    }

    /**
     * Valida performance de operação
     */
    public static boolean isPerformanceAcceptable(Duration actualDuration, Duration maxDuration) {
        return actualDuration.compareTo(maxDuration) <= 0;
    }

    /**
     * Calcula percentil de latência
     */
    public static double calculatePercentile(List<Duration> latencies, double percentile) {
        if (latencies.isEmpty()) return 0.0;
        
        List<Long> sortedLatencies = latencies.stream()
                .mapToLong(Duration::toMillis)
                .sorted()
                .boxed()
                .toList();
        
        int index = (int) Math.ceil((percentile / 100.0) * sortedLatencies.size()) - 1;
        return sortedLatencies.get(Math.max(0, index));
    }
}
