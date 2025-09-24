package com.gra.paradise.botattendance.controller;

import com.gra.paradise.botattendance.model.csv.CsvAnalysisResult;
import com.gra.paradise.botattendance.model.csv.MilitaryPersonnel;
import com.gra.paradise.botattendance.service.csv.CsvAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST controller for CSV analysis operations.
 * Provides endpoints for uploading and analyzing CSV files.
 */
@Slf4j
@RestController
@RequestMapping("/api/csv")
@RequiredArgsConstructor
public class CsvAnalysisController {
    
    private final CsvAnalysisService csvAnalysisService;
    
    /**
     * Analyzes an uploaded CSV file.
     * 
     * @param file the CSV file to analyze
     * @param analysisType the type of analysis to perform (optional, defaults to comprehensive)
     * @return analysis results
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeCsv(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "analysisType", 
                                               defaultValue = "COMPREHENSIVE_MILITARY_ANALYSIS") String analysisType) {
        try {
            log.info("Received CSV analysis request for file: {} with type: {}", 
                    file.getOriginalFilename(), analysisType);
            
            CsvAnalysisResult result = csvAnalysisService.analyzeCsvFile(file, analysisType);
            
            return ResponseEntity.ok(result);
            
        } catch (CsvAnalysisService.CsvAnalysisException e) {
            log.error("CSV analysis error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage(), 
                               "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("Unexpected error during CSV analysis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", 
                               "timestamp", System.currentTimeMillis()));
        }
    }
    
    /**
     * Parses a CSV file without performing analysis.
     * 
     * @param file the CSV file to parse
     * @return list of parsed personnel records
     */
    @PostMapping("/parse")
    public ResponseEntity<?> parseCsv(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received CSV parsing request for file: {}", file.getOriginalFilename());
            
            List<MilitaryPersonnel> personnel = csvAnalysisService.parseCsvFile(file);
            
            return ResponseEntity.ok(Map.of(
                    "personnel", personnel,
                    "totalRecords", personnel.size(),
                    "timestamp", System.currentTimeMillis()
            ));
            
        } catch (CsvAnalysisService.CsvAnalysisException e) {
            log.error("CSV parsing error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage(), 
                               "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("Unexpected error during CSV parsing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", 
                               "timestamp", System.currentTimeMillis()));
        }
    }
    
    /**
     * Gets available analysis types.
     * 
     * @return list of available analysis types
     */
    @GetMapping("/analysis-types")
    public ResponseEntity<List<String>> getAnalysisTypes() {
        try {
            List<String> analysisTypes = csvAnalysisService.getAvailableAnalysisTypes();
            return ResponseEntity.ok(analysisTypes);
        } catch (Exception e) {
            log.error("Error retrieving analysis types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Health check endpoint for the CSV analysis service.
     * 
     * @return service status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "CSV Analysis Service",
                "timestamp", System.currentTimeMillis(),
                "availableAnalysisTypes", csvAnalysisService.getAvailableAnalysisTypes().size()
        ));
    }
}