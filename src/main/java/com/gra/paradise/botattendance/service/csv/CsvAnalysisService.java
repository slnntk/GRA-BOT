package com.gra.paradise.botattendance.service.csv;

import com.gra.paradise.botattendance.model.csv.CsvAnalysisResult;
import com.gra.paradise.botattendance.model.csv.MilitaryPersonnel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Main service for CSV analysis operations.
 * Orchestrates parsing and analysis using design patterns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvAnalysisService {
    
    private final CsvParserFactory parserFactory;
    private final List<CsvAnalysisStrategy> analysisStrategies;
    
    /**
     * Analyzes a CSV file and returns comprehensive results.
     * 
     * @param file the CSV file to analyze
     * @param analysisType the type of analysis to perform
     * @return analysis results
     * @throws CsvAnalysisException if analysis fails
     */
    public CsvAnalysisResult analyzeCsvFile(MultipartFile file, String analysisType) 
            throws CsvAnalysisException {
        
        if (file == null || file.isEmpty()) {
            throw new CsvAnalysisException("File is empty or null");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new CsvAnalysisException("Filename is null");
        }
        
        log.info("Starting CSV analysis for file: {} with type: {}", filename, analysisType);
        
        try {
            // Parse the CSV file
            List<MilitaryPersonnel> personnel = parseCsvFile(file);
            log.info("Successfully parsed {} personnel records", personnel.size());
            
            // Analyze the data
            CsvAnalysisResult result = analyzePersonnelData(personnel, filename, analysisType);
            log.info("Analysis completed successfully");
            
            return result;
            
        } catch (Exception e) {
            log.error("Error during CSV analysis for file: {}", filename, e);
            throw new CsvAnalysisException("Error analyzing CSV file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses CSV file into personnel objects.
     * 
     * @param file the CSV file
     * @return list of personnel objects
     * @throws CsvAnalysisException if parsing fails
     */
    public List<MilitaryPersonnel> parseCsvFile(MultipartFile file) throws CsvAnalysisException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new CsvAnalysisException("Filename is null");
        }
        
        String fileExtension = getFileExtension(filename);
        
        try {
            CsvParserFactory.CsvParser parser = parserFactory.createParser(fileExtension);
            return parser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new CsvAnalysisException("Error reading file: " + e.getMessage(), e);
        } catch (CsvParserFactory.CsvParsingException e) {
            throw new CsvAnalysisException("Error parsing CSV: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new CsvAnalysisException("Unsupported file type: " + fileExtension, e);
        }
    }
    
    /**
     * Analyzes personnel data using the specified strategy.
     * 
     * @param personnel the personnel data
     * @param filename the source filename
     * @param analysisType the type of analysis
     * @return analysis results
     * @throws CsvAnalysisException if analysis fails
     */
    public CsvAnalysisResult analyzePersonnelData(List<MilitaryPersonnel> personnel, 
                                                 String filename, 
                                                 String analysisType) throws CsvAnalysisException {
        
        CsvAnalysisStrategy strategy = findAnalysisStrategy(analysisType);
        return strategy.analyze(personnel, filename);
    }
    
    /**
     * Gets available analysis types.
     * 
     * @return list of available analysis types
     */
    public List<String> getAvailableAnalysisTypes() {
        return analysisStrategies.stream()
                .map(CsvAnalysisStrategy::getAnalysisType)
                .toList();
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        throw new IllegalArgumentException("File has no extension: " + filename);
    }
    
    private CsvAnalysisStrategy findAnalysisStrategy(String analysisType) throws CsvAnalysisException {
        return analysisStrategies.stream()
                .filter(strategy -> strategy.getAnalysisType().equalsIgnoreCase(analysisType))
                .findFirst()
                .orElseThrow(() -> new CsvAnalysisException("Unknown analysis type: " + analysisType));
    }
    
    /**
     * Custom exception for CSV analysis errors.
     */
    public static class CsvAnalysisException extends Exception {
        public CsvAnalysisException(String message) {
            super(message);
        }
        
        public CsvAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}