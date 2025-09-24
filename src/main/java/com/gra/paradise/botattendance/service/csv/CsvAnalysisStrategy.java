package com.gra.paradise.botattendance.service.csv;

import com.gra.paradise.botattendance.model.csv.CsvAnalysisResult;
import com.gra.paradise.botattendance.model.csv.MilitaryPersonnel;

import java.util.List;

/**
 * Strategy interface for different types of CSV analysis.
 * Implements the Strategy design pattern.
 */
public interface CsvAnalysisStrategy {
    
    /**
     * Performs analysis on the given personnel data.
     * 
     * @param personnel the personnel data to analyze
     * @param fileName the name of the source file
     * @return analysis results
     */
    CsvAnalysisResult analyze(List<MilitaryPersonnel> personnel, String fileName);
    
    /**
     * Returns the type of analysis this strategy performs.
     * 
     * @return analysis type name
     */
    String getAnalysisType();
}