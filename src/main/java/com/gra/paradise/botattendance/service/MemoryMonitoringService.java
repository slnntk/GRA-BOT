package com.gra.paradise.botattendance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Serviço de monitoramento de memória para otimização em Railway
 * Monitora uso de memória e fornece logs para debugging
 */
@Service
public class MemoryMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryMonitoringService.class);
    private static final long MB = 1024 * 1024;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logMemoryUsage("Application startup completed");
    }

    /**
     * Monitor memory usage every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void monitorMemoryUsage() {
        logMemoryUsage("Periodic monitoring");
    }

    private void logMemoryUsage(String context) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double usedPercent = (double) usedMemory / maxMemory * 100;
        
        logger.info("Memory Status [{}] - Used: {}MB ({}%), Free: {}MB, Max: {}MB", 
                context,
                usedMemory / MB, 
                String.format("%.1f", usedPercent),
                freeMemory / MB, 
                maxMemory / MB);
        
        // Warning if memory usage is too high
        if (usedPercent > 80) {
            logger.warn("High memory usage detected: {}% - Consider optimization", 
                    String.format("%.1f", usedPercent));
        }
        
        // Suggest GC if memory usage is high but not critical
        if (usedPercent > 70 && usedPercent <= 80) {
            System.gc(); // Suggest garbage collection
            logger.info("Suggested garbage collection due to memory pressure");
        }
    }

    /**
     * Get current memory statistics as string for diagnostic purposes
     */
    public String getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return String.format("Memory: %dMB/%dMB (%.1f%% used)", 
                usedMemory / MB, 
                maxMemory / MB,
                (double) usedMemory / maxMemory * 100);
    }
}