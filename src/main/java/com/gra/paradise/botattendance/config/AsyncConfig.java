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
     * Configurado para balancear performance e uso de memória em ambiente Railway
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Configurações otimizadas para Railway - menor uso de memória
        executor.setCorePoolSize(2);   // Reduzido de 4 para 2 threads mínimas
        executor.setMaxPoolSize(6);    // Reduzido de 10 para 6 threads máximas
        executor.setQueueCapacity(15); // Reduzido de 25 para 15 fila de tarefas
        executor.setThreadNamePrefix("GRA-Bot-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        // Configuração adicional para reduzir overhead de memória
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }
}