package com.gra.paradise.botattendance.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuração de Performance e Cache
 * Otimiza uso de memória e CPU com cache inteligente
 */
@Configuration
@EnableCaching
@EnableScheduling
public class PerformanceConfig {

    /**
     * Cache para mensagens Discord - evita recriar embeds desnecessariamente
     * TTL: 5 minutos, máximo 1000 entradas
     */
    @Bean
    public Cache<String, Object> discordMessageCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * Cache para configurações de guild - dados que mudam raramente
     * TTL: 1 hora, máximo 100 entradas
     */
    @Bean
    public Cache<String, Object> guildConfigCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    /**
     * Cache para usuários - dados que mudam raramente
     * TTL: 2 horas, máximo 5000 entradas
     */
    @Bean
    public Cache<String, Object> userCache() {
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    /**
     * Cache Manager principal
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats());
        return cacheManager;
    }
}
