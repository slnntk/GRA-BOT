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
     * Configurado para balancear performance e uso de memória
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Configurações otimizadas para bot Discord com operações de I/O
        executor.setCorePoolSize(4);  // Threads mínimas
        executor.setMaxPoolSize(10);  // Threads máximas - evita sobrecarga de memória
        executor.setQueueCapacity(25); // Fila de tarefas
        executor.setThreadNamePrefix("GRA-Bot-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}