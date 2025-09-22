package com.gra.paradise.botattendance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller para funcionalidades da Calculadora Penal
 * 
 * @author Tiago Holanda
 */
@RestController
@RequestMapping("/api/calculadora")
@CrossOrigin(origins = "*")
public class CalculadoraPenalApiController {

    private static final ZoneId FORTALEZA_ZONE = ZoneId.of("America/Fortaleza");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Endpoint para obter a data/hora atual do sistema
     */
    @GetMapping("/datetime")
    public ResponseEntity<Map<String, Object>> getCurrentDateTime() {
        Map<String, Object> response = new HashMap<>();
        LocalDateTime now = LocalDateTime.now(FORTALEZA_ZONE);
        
        response.put("datetime", now.format(DATE_TIME_FORMATTER));
        response.put("timestamp", now);
        response.put("timezone", "America/Fortaleza");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para validar dados do formulário
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateFormData(@RequestBody Map<String, Object> formData) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        // Validar nome do agente
        String agentName = (String) formData.get("agentName");
        if (agentName == null || agentName.trim().isEmpty()) {
            errors.put("agentName", "Nome do agente é obrigatório");
        } else if (agentName.trim().length() < 3) {
            errors.put("agentName", "Nome deve ter pelo menos 3 caracteres");
        }
        
        // Validar número da placa
        String agentBadge = (String) formData.get("agentBadge");
        if (agentBadge == null || agentBadge.trim().isEmpty()) {
            errors.put("agentBadge", "Número da placa é obrigatório");
        }
        
        // Validar tipo de veículo
        String vehicleType = (String) formData.get("vehicleType");
        if (vehicleType == null || vehicleType.trim().isEmpty()) {
            errors.put("vehicleType", "Tipo de veículo é obrigatório");
        }
        
        // Validar tipo de missão
        String missionType = (String) formData.get("missionType");
        if (missionType == null || missionType.trim().isEmpty()) {
            errors.put("missionType", "Tipo de missão é obrigatório");
        }
        
        boolean isValid = errors.isEmpty();
        
        response.put("valid", isValid);
        response.put("errors", errors);
        response.put("timestamp", LocalDateTime.now(FORTALEZA_ZONE).format(DATE_TIME_FORMATTER));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para calcular penalidade
     */
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculatePenalty(@RequestBody Map<String, Object> calculationData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Processar dados de entrada
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> infractions = (java.util.List<Map<String, Object>>) calculationData.get("infractions");
            
            int totalPoints = 0;
            if (infractions != null) {
                totalPoints = infractions.stream()
                    .mapToInt(infraction -> ((Number) infraction.get("points")).intValue())
                    .sum();
            }
            
            // Determinar nível de penalidade
            String penaltyLevel;
            String penaltyDescription;
            
            if (totalPoints >= 10) {
                penaltyLevel = "high";
                penaltyDescription = "Suspensão Disciplinar";
            } else if (totalPoints >= 5) {
                penaltyLevel = "medium";
                penaltyDescription = "Advertência Escrita";
            } else {
                penaltyLevel = "low";
                penaltyDescription = "Advertência Verbal";
            }
            
            response.put("success", true);
            response.put("totalPoints", totalPoints);
            response.put("penaltyLevel", penaltyLevel);
            response.put("penaltyDescription", penaltyDescription);
            response.put("calculationDate", LocalDateTime.now(FORTALEZA_ZONE).format(DATE_TIME_FORMATTER));
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erro ao processar cálculo: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para obter estatísticas do sistema
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        Map<String, Object> response = new HashMap<>();
        
        // Dados simulados para demonstração
        response.put("totalCalculations", 0);
        response.put("systemUptime", "Sistema iniciado");
        response.put("lastCalculation", null);
        response.put("timestamp", LocalDateTime.now(FORTALEZA_ZONE).format(DATE_TIME_FORMATTER));
        
        return ResponseEntity.ok(response);
    }
}