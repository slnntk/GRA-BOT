package com.gra.paradise.botattendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuração para operações assíncronas - otimiza performance de operações não-blocking
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Pool de threads otimizado para operações Discord e banco de dados
     * Configurado para Railway deployment com recursos limitados
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Configurações otimizadas para Railway com menos recursos
        executor.setCorePoolSize(2);  // Threads mínimas reduzidas
        executor.setMaxPoolSize(6);   // Threads máximas reduzidas para economizar memória
        executor.setQueueCapacity(20); // Fila menor para resposta mais rápida
        executor.setThreadNamePrefix("GRA-Bot-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15); // Tempo reduzido para shutdown mais rápido
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}