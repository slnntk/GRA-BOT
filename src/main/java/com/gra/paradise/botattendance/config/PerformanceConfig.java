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
     * TTL: 2 minutos, máximo 50 entradas (otimizado para baixo uso de memória)
     */
    @Bean
    public Cache<String, Object> discordMessageCache() {
        return Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * Cache para configurações de guild - dados que mudam raramente
     * TTL: 30 minutos, máximo 20 entradas (otimizado para baixo uso de memória)
     */
    @Bean
    public Cache<String, Object> guildConfigCache() {
        return Caffeine.newBuilder()
                .maximumSize(20)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * Cache para usuários - dados que mudam raramente
     * TTL: 1 hora, máximo 100 entradas (otimizado para baixo uso de memória)
     */
    @Bean
    public Cache<String, Object> userCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .expireAfterAccess(30, TimeUnit.MINUTES)
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
                .maximumSize(50)
                .expireAfterWrite(Duration.ofMinutes(2))
                .expireAfterAccess(Duration.ofMinutes(1))
                .recordStats());
        return cacheManager;
    }
}
