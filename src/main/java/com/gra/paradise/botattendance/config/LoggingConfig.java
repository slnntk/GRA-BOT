package com.gra.paradise.botattendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Configuração de Logging Otimizada
 * Gerencia logs de forma eficiente para melhor performance
 */
@Configuration
@EnableScheduling
public class LoggingConfig {

    /**
     * Executor para logs assíncronos
     * Evita bloqueio de threads principais
     */
    @Bean(name = "loggingExecutor")
    public ScheduledExecutorService loggingExecutor() {
        return Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "async-logging");
            t.setDaemon(true);
            return t;
        });
    }
}
