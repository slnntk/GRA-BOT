package com.gra.paradise.botattendance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Serviço de Modo Stand-by
 * Gerencia o bot para economizar recursos quando não há atividade
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StandbyService {

    private final PerformanceMetricsService performanceMetrics;
    private final CacheService cacheService;
    
    private final AtomicBoolean isStandby = new AtomicBoolean(false);
    private final AtomicLong lastActivity = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong standbyStartTime = new AtomicLong(0);
    
    // Configurações de stand-by
    private static final long STANDBY_TIMEOUT = 3 * 60 * 1000; // 3 minutos
    private static final long ACTIVITY_CHECK_INTERVAL = 1 * 60 * 1000; // 1 minuto

    /**
     * Registra atividade do usuário
     */
    public void recordActivity() {
        lastActivity.set(System.currentTimeMillis());
        if (isStandby.get()) {
            exitStandby();
        }
    }

    /**
     * Verifica se deve entrar em modo stand-by
     */
    @Scheduled(fixedRate = ACTIVITY_CHECK_INTERVAL)
    public void checkStandbyCondition() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastActivity = currentTime - lastActivity.get();
        
        if (timeSinceLastActivity > STANDBY_TIMEOUT && !isStandby.get()) {
            enterStandby();
        } else if (timeSinceLastActivity <= STANDBY_TIMEOUT && isStandby.get()) {
            exitStandby();
        }
    }

    /**
     * Entra em modo stand-by
     */
    private void enterStandby() {
        isStandby.set(true);
        standbyStartTime.set(System.currentTimeMillis());
        
        // Limpa cache para liberar memória
        cacheService.clear();
        
        // Força garbage collection agressivo
        System.gc();
        System.runFinalization();
        System.gc();
        
        // Limpa métricas antigas para liberar memória
        performanceMetrics.clearOldMetrics();
        
        log.info("Bot entered standby mode - resources optimized and memory freed");
    }

    /**
     * Sai do modo stand-by
     */
    private void exitStandby() {
        if (isStandby.get()) {
            long standbyDuration = System.currentTimeMillis() - standbyStartTime.get();
            isStandby.set(false);
            
            log.info("Bot exited standby mode after {} minutes", 
                standbyDuration / (60 * 1000));
        }
    }

    /**
     * Verifica se está em stand-by
     */
    public boolean isStandby() {
        return isStandby.get();
    }

    /**
     * Obtém tempo desde última atividade
     */
    public long getTimeSinceLastActivity() {
        return System.currentTimeMillis() - lastActivity.get();
    }

    /**
     * Obtém tempo em stand-by
     */
    public long getStandbyDuration() {
        if (isStandby.get()) {
            return System.currentTimeMillis() - standbyStartTime.get();
        }
        return 0;
    }

    /**
     * Obtém status do stand-by
     */
    public String getStandbyStatus() {
        if (isStandby.get()) {
            long standbyMinutes = getStandbyDuration() / (60 * 1000);
            return String.format("Standby Mode - %d minutes", standbyMinutes);
        } else {
            long inactiveMinutes = getTimeSinceLastActivity() / (60 * 1000);
            return String.format("Active Mode - %d minutes since last activity", inactiveMinutes);
        }
    }

    /**
     * Força entrada em stand-by (para testes)
     */
    public void forceStandby() {
        enterStandby();
    }

    /**
     * Força saída do stand-by
     */
    public void forceActive() {
        recordActivity();
    }
}
